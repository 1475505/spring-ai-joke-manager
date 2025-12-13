package com.duduk.jokemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 评论相关DTO类
 */
public class CommentDto {
    
    /**
     * 评论信息
     */
    @Schema(description = "评论信息")
    public static class CommentInfo {
        @Schema(description = "评论ID", example = "1")
        private Long id;
        
        @Schema(description = "评论内容", example = "这个主题很有趣！")
        private String content;
        
        @Schema(description = "作者信息")
        private AuthorInfo author;
        
        @Schema(description = "匿名作者名称", example = "游客用户")
        private String authorName;
        
        @Schema(description = "所属主题")
        private ThemeInfo theme;
        
        @Schema(description = "所属笑话")
        private JokeInfo joke;
        
        @Schema(description = "统计信息")
        private StatisticsInfo statistics;
        
        @Schema(description = "当前用户是否可编辑", example = "false")
        private Boolean canEdit;
        
        @Schema(description = "当前用户是否可删除", example = "false")
        private Boolean canDelete;
        
        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
        
        public CommentInfo() {}
        
        public CommentInfo(Long id, String content, AuthorInfo author, String authorName,
                          ThemeInfo theme, JokeInfo joke, StatisticsInfo statistics, Boolean canEdit, 
                          Boolean canDelete, LocalDateTime createdAt) {
            this.id = id;
            this.content = content;
            this.author = author;
            this.authorName = authorName;
            this.theme = theme;
            this.joke = joke;
            this.statistics = statistics;
            this.canEdit = canEdit;
            this.canDelete = canDelete;
            this.createdAt = createdAt;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public AuthorInfo getAuthor() { return author; }
        public void setAuthor(AuthorInfo author) { this.author = author; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public ThemeInfo getTheme() { return theme; }
        public void setTheme(ThemeInfo theme) { this.theme = theme; }
        public JokeInfo getJoke() { return joke; }
        public void setJoke(JokeInfo joke) { this.joke = joke; }
        public StatisticsInfo getStatistics() { return statistics; }
        public void setStatistics(StatisticsInfo statistics) { this.statistics = statistics; }
        public Boolean getCanEdit() { return canEdit; }
        public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }
        public Boolean getCanDelete() { return canDelete; }
        public void setCanDelete(Boolean canDelete) { this.canDelete = canDelete; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 作者信息
     */
    @Schema(description = "作者信息")
    public static class AuthorInfo {
        @Schema(description = "作者ID", example = "1")
        private Long id;
        
        @Schema(description = "作者用户名", example = "commenter")
        private String username;
        
        @Schema(description = "作者头像URL")
        private String avatarUrl;
        
        public AuthorInfo() {}
        
        public AuthorInfo(Long id, String username, String avatarUrl) {
            this.id = id;
            this.username = username;
            this.avatarUrl = avatarUrl;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    /**
     * 主题信息
     */
    @Schema(description = "主题信息")
    public static class ThemeInfo {
        @Schema(description = "主题ID", example = "1")
        private Long id;
        
        @Schema(description = "主题名称", example = "搞笑段子")
        private String name;
        
        public ThemeInfo() {}
        
        public ThemeInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    /**
     * 笑话信息
     */
    @Schema(description = "笑话信息")
    public static class JokeInfo {
        @Schema(description = "笑话ID", example = "1")
        private Long id;
        
        @Schema(description = "笑话标题", example = "搞笑段子")
        private String title;
        
        public JokeInfo() {}
        
        public JokeInfo(Long id, String title) {
            this.id = id;
            this.title = title;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }
    
    /**
     * 统计信息
     */
    @Schema(description = "统计信息")
    public static class StatisticsInfo {
        @Schema(description = "点赞次数", example = "5")
        private Integer likeCount;
        
        @Schema(description = "回复次数", example = "2")
        private Integer replyCount;
        
        public StatisticsInfo() {}
        
        public StatisticsInfo(Integer likeCount, Integer replyCount) {
            this.likeCount = likeCount;
            this.replyCount = replyCount;
        }
        
        public Integer getLikeCount() { return likeCount; }
        public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
        public Integer getReplyCount() { return replyCount; }
        public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }
    }
    
    /**
     * 创建评论请求
     */
    @Schema(description = "创建评论请求")
    public static class CreateCommentRequest {
        @Schema(description = "评论内容", example = "这个主题很有趣！", required = true)
        @NotBlank(message = "评论内容不能为空")
        @Size(min = 3, max = 500, message = "评论内容长度必须在3-500字符之间")
        private String content;
        
        @Schema(description = "匿名评论时的显示名称", example = "游客用户")
        @Size(max = 20, message = "显示名称长度不能超过20字符")
        private String authorName;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
    }
}