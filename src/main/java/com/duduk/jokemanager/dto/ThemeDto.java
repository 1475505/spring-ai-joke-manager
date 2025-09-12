package com.duduk.jokemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 主题相关DTO类
 */
public class ThemeDto {
    
    /**
     * 主题信息
     */
    @Schema(description = "主题信息")
    public static class ThemeInfo {
        @Schema(description = "主题ID", example = "1")
        private Long id;
        
        @Schema(description = "主题名称", example = "搞笑段子")
        private String name;
        
        @Schema(description = "AI生成笑话的特征描述", example = "生成关于程序员日常工作的搞笑段子")
        private String prompt;
        
        @Schema(description = "主题图标", example = "😄")
        private String icon;
        
        @Schema(description = "创建者信息")
        private UserInfo createdBy;
        
        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
        
        @Schema(description = "更新时间")
        private LocalDateTime updatedAt;
        
        public ThemeInfo() {}
        
        public ThemeInfo(Long id, String name, String prompt, String icon, 
                        UserInfo createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.prompt = prompt;
            this.icon = icon;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public UserInfo getCreatedBy() { return createdBy; }
        public void setCreatedBy(UserInfo createdBy) { this.createdBy = createdBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
    
    /**
     * 创建主题请求
     */
    @Schema(description = "创建主题请求")
    public static class CreateThemeRequest {
        @Schema(description = "主题名称", example = "搞笑段子", required = true)
        @NotBlank(message = "主题名称不能为空")
        @Size(min = 2, max = 50, message = "主题名称长度必须在2-50字符之间")
        private String name;
        
        @Schema(description = "AI生成笑话的特征描述", example = "生成关于程序员日常工作的搞笑段子")
        @Size(max = 500, message = "AI生成提示词长度不能超过500字符")
        private String prompt;
        
        @Schema(description = "主题图标", example = "😄")
        private String icon;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
    
    /**
     * 更新主题请求
     */
    @Schema(description = "更新主题请求")
    public static class UpdateThemeRequest {
        @Schema(description = "主题名称", example = "搞笑段子")
        @Size(min = 2, max = 50, message = "主题名称长度必须在2-50字符之间")
        private String name;
        
        @Schema(description = "AI生成笑话的特征描述", example = "生成关于程序员日常工作的搞笑段子")
        @Size(max = 500, message = "AI生成提示词长度不能超过500字符")
        private String prompt;
        
        @Schema(description = "主题图标", example = "😄")
        private String icon;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
    
    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID", example = "1")
        private Long id;
        
        @Schema(description = "用户名", example = "admin")
        private String username;
        
        public UserInfo() {}
        
        public UserInfo(Long id, String username) {
            this.id = id;
            this.username = username;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}