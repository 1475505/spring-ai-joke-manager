package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.CommentDto;
import com.duduk.jokemanager.dto.PageResponse;
import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.CommentRepository;
import com.duduk.jokemanager.repository.JokeRepository;
import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.service.CommentService;
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
@RequestMapping("/jokes/{jokeId}/comments")
@Tag(name = "笑话评论", description = "笑话评论相关接口")
public class JokeCommentController {

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentService commentService;

    @Autowired
    private JokeRepository jokeRepository;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "获取笑话评论列表", description = "分页获取指定笑话的评论列表")
    public ResponseEntity<ApiResponse<PageResponse<CommentDto.CommentInfo>>> getJokeComments(
            @PathVariable Long jokeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword) {
        
        try {
            Optional<Joke> jokeOpt = jokeRepository.findById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            Joke joke = jokeOpt.get();
            
            if (size > 50) size = 50;
            
            Sort sortObj = Sort.by("desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, sort);
            Pageable pageable = PageRequest.of(page, size, sortObj);
            
            Page<Comment> comments;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                comments = commentRepository.findByJokeAndKeyword(joke, keyword.trim(), pageable);
            } else {
                comments = commentRepository.findByJoke(joke, pageable);
            }
            
            Long currentUserId = getCurrentUserId();
            
            Page<CommentDto.CommentInfo> commentInfoPage = comments.map(comment -> 
                convertToCommentInfo(comment, currentUserId)
            );
            
            PageResponse<CommentDto.CommentInfo> response = PageResponse.of(commentInfoPage);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取笑话评论列表失败：" + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "创建笑话评论", description = "为指定笑话创建新评论")
    public ResponseEntity<ApiResponse<CommentDto.CommentInfo>> createJokeComment(
            @PathVariable Long jokeId,
            @Valid @RequestBody CommentDto.CreateCommentRequest request) {
        
        try {
            Optional<Joke> jokeOpt = jokeRepository.findById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            Joke joke = jokeOpt.get();
            
            Comment comment;
            Long currentUserId = getCurrentUserId();
            
            if (currentUserId != null) {
                // 登录用户评论
                Optional<User> userOpt = userService.findById(currentUserId);
                if (userOpt.isEmpty()) {
                    return ResponseEntity.status(404)
                            .body(ApiResponse.error(404, "用户不存在"));
                }
                
                User user = userOpt.get();
                comment = commentService.createJokeComment(request.getContent(), joke, user);
                // 如果登录用户提供了自定义昵称，则使用自定义昵称
                if (request.getAuthorName() != null && !request.getAuthorName().trim().isEmpty()) {
                    comment.setAuthorName(request.getAuthorName().trim());
                    comment = commentRepository.save(comment);
                }
            } else {
                // 匿名评论
                if (request.getAuthorName() == null || request.getAuthorName().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error(400, "匿名评论必须提供显示名称"));
                }
                
                comment = commentService.createJokeComment(request.getContent(), joke, request.getAuthorName().trim());
            }
            
            CommentDto.CommentInfo commentInfo = convertToCommentInfo(comment, currentUserId);
            return ResponseEntity.ok(ApiResponse.success(commentInfo, "评论创建成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "创建笑话评论失败：" + e.getMessage()));
        }
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论详情", description = "获取指定评论的详细信息")
    public ResponseEntity<ApiResponse<CommentDto.CommentInfo>> getComment(
            @PathVariable Long jokeId,
            @PathVariable Long commentId) {
        
        try {
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            Comment comment = commentOpt.get();
            
            // 验证评论是否属于指定笑话
            if (!comment.getJoke().getId().equals(jokeId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "评论不属于指定笑话"));
            }
            
            Long currentUserId = getCurrentUserId();
            CommentDto.CommentInfo commentInfo = convertToCommentInfo(comment, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(commentInfo));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取评论详情失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "更新评论内容", description = "更新指定评论的内容（仅作者可操作）")
    public ResponseEntity<ApiResponse<CommentDto.CommentInfo>> updateComment(
            @PathVariable Long jokeId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDto.CreateCommentRequest request) {
        
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            Comment comment = commentOpt.get();
            
            // 验证评论是否属于指定笑话
            if (!comment.getJoke().getId().equals(jokeId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "评论不属于指定笑话"));
            }
            
            // 验证权限：只有作者可以修改
            if (comment.getUser() == null || !comment.getUser().getId().equals(currentUserId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "只能修改自己的评论"));
            }
            
            Comment updatedComment = commentService.updateComment(commentId, request.getContent());
            CommentDto.CommentInfo commentInfo = convertToCommentInfo(updatedComment, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(commentInfo, "评论更新成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "更新评论失败：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定评论（作者或管理员可操作）")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long jokeId,
            @PathVariable Long commentId) {
        
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            Comment comment = commentOpt.get();
            
            // 验证评论是否属于指定笑话
            if (!comment.getJoke().getId().equals(jokeId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "评论不属于指定笑话"));
            }
            
            // 验证权限：作者或ROOT用户可以删除
            boolean canDelete = false;
            if (comment.getUser() != null && comment.getUser().getId().equals(currentUserId)) {
                canDelete = true;
            } else {
                // 检查是否为ROOT用户
                Optional<User> userOpt = userService.findById(currentUserId);
                if (userOpt.isPresent() && userOpt.get().isRoot()) {
                    canDelete = true;
                }
            }
            
            if (!canDelete) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "没有权限删除此评论"));
            }
            
            commentService.deleteById(commentId);
            return ResponseEntity.ok(ApiResponse.success("评论删除成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除评论失败：" + e.getMessage()));
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
        
        CommentDto.ThemeInfo themeInfo = null;
        if (comment.getTheme() != null) {
            themeInfo = new CommentDto.ThemeInfo(
                    comment.getTheme().getId(),
                    comment.getTheme().getName()
            );
        }
        
        CommentDto.JokeInfo jokeInfo = null;
        if (comment.getJoke() != null) {
            jokeInfo = new CommentDto.JokeInfo(
                    comment.getJoke().getId(),
                    comment.getJoke().getTitle() != null ? comment.getJoke().getTitle() : "无标题"
            );
        }
        
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
                jokeInfo,
                statisticsInfo,
                canEdit,
                canDelete,
                comment.getCreatedAt()
        );
    }
}