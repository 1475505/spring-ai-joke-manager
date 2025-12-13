package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.CommentDto;
import com.duduk.jokemanager.dto.PageResponse;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.CommentRepository;
import com.duduk.jokemanager.service.CommentService;
import com.duduk.jokemanager.repository.ThemeRepository;
import com.duduk.jokemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/themes/{themeId}/comments")
@Tag(name = "主题评论", description = "主题评论相关接口")
public class ThemeCommentController {

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentService commentService;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "获取主题评论列表", description = "分页获取指定主题的评论列表")
    public ResponseEntity<ApiResponse<PageResponse<CommentDto.CommentInfo>>> getThemeComments(
            @PathVariable Long themeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword) {
        
        try {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            Theme theme = themeOpt.get();
            
            if (size > 50) size = 50;
            
            Sort sortObj = Sort.by("desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, sort);
            Pageable pageable = PageRequest.of(page, size, sortObj);
            
            Page<Comment> comments;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                comments = commentRepository.findByThemeAndKeyword(theme, keyword.trim(), pageable);
            } else {
                comments = commentRepository.findByTheme(theme, pageable);
            }
            
            // 获取当前用户信息用于权限判断
            Long currentUserId = getCurrentUserId();
            
            Page<CommentDto.CommentInfo> commentInfoPage = comments.map(comment -> 
                convertToCommentInfo(comment, currentUserId)
            );
            
            PageResponse<CommentDto.CommentInfo> response = PageResponse.of(commentInfoPage);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取评论列表失败：" + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "发表主题评论", description = "对指定主题发表评论（支持匿名评论）")
    public ResponseEntity<ApiResponse<CommentDto.CommentInfo>> createComment(
            @PathVariable Long themeId,
            @Valid @RequestBody CommentDto.CreateCommentRequest request) {
        
        try {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            Theme theme = themeOpt.get();
            Comment comment;
            
            // 检查是否已登录
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails) {
                // 已登录用户评论
                JwtAuthenticationFilter.JwtUserDetails userDetails = 
                        (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
                
                Optional<User> userOpt = userService.findById(userDetails.getUserId());
                if (userOpt.isPresent()) {
                    comment = new Comment(request.getContent(), theme, userOpt.get());
                    // 如果登录用户提供了自定义昵称，则使用自定义昵称
                    if (request.getAuthorName() != null && !request.getAuthorName().trim().isEmpty()) {
                        comment.setAuthorName(request.getAuthorName().trim());
                    }
                } else {
                    return ResponseEntity.status(404)
                            .body(ApiResponse.error(404, "用户不存在"));
                }
            } else {
                // 匿名评论
                String authorName = request.getAuthorName();
                if (authorName == null || authorName.trim().isEmpty()) {
                    authorName = "匿名用户";
                }
                comment = new Comment(request.getContent(), theme, authorName);
            }
            
            Comment savedComment = commentRepository.save(comment);
            
            Long currentUserId = getCurrentUserId();
            CommentDto.CommentInfo commentInfo = convertToCommentInfo(savedComment, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(commentInfo, "评论发表成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "发表评论失败：" + e.getMessage()));
        }
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论详情", description = "获取指定评论的详细信息")
    public ResponseEntity<ApiResponse<CommentDto.CommentInfo>> getComment(
            @PathVariable Long themeId,
            @PathVariable Long commentId) {
        
        try {
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            Comment comment = commentOpt.get();
            
            // 验证评论是否属于指定主题
            if (!comment.getTheme().getId().equals(themeId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "评论不属于指定主题"));
            }
            
            Long currentUserId = getCurrentUserId();
            CommentDto.CommentInfo commentInfo = convertToCommentInfo(comment, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(commentInfo));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取评论详情失败：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定评论（作者或管理员）")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long themeId,
            @PathVariable Long commentId) {
        
        try {
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            Comment comment = commentOpt.get();
            
            // 验证评论是否属于指定主题
            if (!comment.getTheme().getId().equals(themeId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "评论不属于指定主题"));
            }
            
            // 权限检查
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "需要登录才能删除评论"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<User> userOpt = userService.findById(userDetails.getUserId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User user = userOpt.get();
            boolean canDelete = false;
            
            // ROOT用户可以删除任何评论
            if (user.isRoot()) {
                canDelete = true;
            }
            // 评论作者可以删除自己的评论
            else if (comment.getUser() != null && comment.getUser().getId().equals(user.getId())) {
                canDelete = true;
            }
            // TODO: 添加主题管理员权限检查
            
            if (!canDelete) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，只能删除自己的评论"));
            }
            
            commentRepository.delete(comment);
            return ResponseEntity.ok(ApiResponse.success("评论删除成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除评论失败：" + e.getMessage()));
        }
    }

    // ==================== 管理员功能 ====================
    
    @GetMapping("/management")
    @Operation(summary = "获取评论管理列表", description = "获取主题的评论管理列表（管理员功能）")
    public ResponseEntity<ApiResponse<PageResponse<CommentDto.CommentInfo>>> getCommentsForManagement(
            @PathVariable Long themeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // TODO: 添加管理员权限检查
            
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            Theme theme = themeOpt.get();
            
            if (size > 100) size = 100;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            Page<Comment> comments;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                comments = commentRepository.findByThemeAndKeyword(theme, keyword.trim(), pageable);
            } else {
                comments = commentRepository.findByTheme(theme, pageable);
            }
            
            Long currentUserId = getCurrentUserId();
            
            Page<CommentDto.CommentInfo> commentInfoPage = comments.map(comment -> 
                convertToCommentInfo(comment, currentUserId)
            );
            
            PageResponse<CommentDto.CommentInfo> response = PageResponse.of(commentInfoPage);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取评论管理列表失败：" + e.getMessage()));
        }
    }

    // ==================== 私有辅助方法 ====================
    
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails) {
                JwtAuthenticationFilter.JwtUserDetails userDetails = 
                        (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
                return userDetails.getUserId();
            }
        } catch (Exception e) {
            // 忽略异常，返回null表示未登录
        }
        return null;
    }
    
    private CommentDto.CommentInfo convertToCommentInfo(Comment comment, Long currentUserId) {
        CommentDto.AuthorInfo authorInfo = null;
        if (comment.getUser() != null) {
            authorInfo = new CommentDto.AuthorInfo(
                    comment.getUser().getId(),
                    comment.getUser().getUserName(),
                    comment.getUser().getAvatarUrl()
            );
        }
        
        CommentDto.ThemeInfo themeInfo = new CommentDto.ThemeInfo(
                comment.getTheme().getId(),
                comment.getTheme().getName()
        );
        
        CommentDto.StatisticsInfo statisticsInfo = new CommentDto.StatisticsInfo(
                0, // TODO: 实现点赞功能
                0  // TODO: 实现回复功能
        );
        
        // 权限判断
        boolean canEdit = false;
        boolean canDelete = false;
        
        if (currentUserId != null) {
            // 作者可以编辑和删除自己的评论
            if (comment.getUser() != null && comment.getUser().getId().equals(currentUserId)) {
                canEdit = true;
                canDelete = true;
            }
            
            // TODO: 添加管理员权限检查
            // ROOT用户可以删除任何评论
            try {
                Optional<User> userOpt = userService.findById(currentUserId);
                if (userOpt.isPresent() && userOpt.get().isRoot()) {
                    canDelete = true;
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        
        return new CommentDto.CommentInfo(
                comment.getId(),
                comment.getContent(),
                authorInfo,
                comment.getAuthorName(),
                themeInfo,
                null, // jokeInfo - 主题评论没有关联笑话
                statisticsInfo,
                canEdit,
                canDelete,
                comment.getCreatedAt()
        );
    }
}