package com.duduk.jokemanager.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.CommentDto;
import com.duduk.jokemanager.dto.PageResponse;
import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.CommentRepository;
import com.duduk.jokemanager.service.CommentService;
import com.duduk.jokemanager.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/comments")
@Tag(name = "评论管理", description = "评论管理相关接口")
public class CommentController {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    @Operation(summary = "获取评论管理列表", description = "获取评论管理列表（管理员功能）")
    public ResponseEntity<ApiResponse<PageResponse<CommentDto.CommentInfo>>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long themeId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // TODO: 添加管理员权限检查
            
            if (size > 100) size = 100;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // 简化实现，实际应该根据参数过滤
            Page<Comment> comments = commentRepository.findAll(pageable);
            
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
    
    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论详情", description = "获取指定评论的详细信息")
    public ResponseEntity<ApiResponse<CommentDto.CommentInfo>> getComment(@PathVariable Long commentId) {
        try {
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            Comment comment = commentOpt.get();
            Long currentUserId = getCurrentUserId();
            CommentDto.CommentInfo commentInfo = convertToCommentInfo(comment, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(commentInfo));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取评论详情失败：" + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定评论（管理员功能）")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable Long commentId) {
        try {
            // TODO: 添加管理员权限检查
            
            Optional<Comment> commentOpt = commentRepository.findById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "评论不存在"));
            }
            
            commentRepository.deleteById(commentId);
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
                0,
                0  // TODO
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