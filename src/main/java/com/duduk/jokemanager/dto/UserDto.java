package com.duduk.jokemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户相关DTO类
 */
public class UserDto {
    
    /**
     * 登录请求
     */
    @Schema(description = "登录请求")
    public static class LoginRequest {
        @Schema(description = "用户名", example = "admin", required = true)
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @Schema(description = "密码", example = "password", required = true)
        @NotBlank(message = "密码不能为空")
        private String password;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * 登录响应
     */
    @Schema(description = "登录响应")
    public static class LoginResponse {
        @Schema(description = "用户信息")
        private UserInfo user;
        
        @Schema(description = "访问令牌")
        private String accessToken;
        
        @Schema(description = "刷新令牌")
        private String refreshToken;
        
        @Schema(description = "过期时间(秒)", example = "3600")
        private Long expiresIn;
        
        public LoginResponse() {}
        
        public LoginResponse(UserInfo user, String accessToken, String refreshToken, Long expiresIn) {
            this.user = user;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
        
        public UserInfo getUser() { return user; }
        public void setUser(UserInfo user) { this.user = user; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
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
        
        @Schema(description = "用户角色", example = "ROOT")
        private String role;
        
        @Schema(description = "头像URL")
        private String avatarUrl;
        
        public UserInfo() {}
        
        public UserInfo(Long id, String username, String role, String avatarUrl) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.avatarUrl = avatarUrl;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    /**
     * 创建用户请求
     */
    @Schema(description = "创建用户请求")
    public static class CreateUserRequest {
        @Schema(description = "用户名", example = "newuser", required = true)
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
        private String username;
        

        
        @Schema(description = "密码", example = "password123", required = true)
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100字符之间")
        private String password;
        
        @Schema(description = "用户角色", example = "USER", allowableValues = {"ROOT", "USER"})
        private String role = "USER";
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
    
    /**
     * 更新用户信息请求
     */
    @Schema(description = "更新用户信息请求")
    public static class UpdateUserRequest {
        @Schema(description = "用户名", example = "newusername")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
        private String username;
        
        @Schema(description = "用户角色", example = "USER", allowableValues = {"ROOT", "USER"})
        private String role;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
    
    /**
     * 修改密码请求
     */
    @Schema(description = "修改密码请求")
    public static class ChangePasswordRequest {
        @Schema(description = "旧密码", example = "oldpass", required = true)
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;
        
        @Schema(description = "新密码", example = "newpass", required = true)
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 100, message = "新密码长度必须在6-100字符之间")
        private String newPassword;
        
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    /**
     * 更新个人信息请求
     */
    @Schema(description = "更新个人信息请求")
    public static class UpdateProfileRequest {
        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        private String avatarUrl;
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    /**
     * Token刷新请求
     */
    @Schema(description = "Token刷新请求")
    public static class RefreshTokenRequest {
        @Schema(description = "刷新令牌", required = true)
        @NotBlank(message = "刷新令牌不能为空")
        private String refreshToken;
        
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
    
    /**
     * 用户权限信息
     */
    @Schema(description = "用户权限信息")
    public static class UserPermissions {
        @Schema(description = "用户ID")
        private Long userId;
        
        @Schema(description = "全局角色")
        private String globalRole;
        
        @Schema(description = "主题权限列表")
        private java.util.List<ThemePermissionInfo> themePermissions;
        
        public UserPermissions() {}
        
        public UserPermissions(Long userId, String globalRole, java.util.List<ThemePermissionInfo> themePermissions) {
            this.userId = userId;
            this.globalRole = globalRole;
            this.themePermissions = themePermissions;
        }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getGlobalRole() { return globalRole; }
        public void setGlobalRole(String globalRole) { this.globalRole = globalRole; }
        public java.util.List<ThemePermissionInfo> getThemePermissions() { return themePermissions; }
        public void setThemePermissions(java.util.List<ThemePermissionInfo> themePermissions) { this.themePermissions = themePermissions; }
    }
    
    /**
     * 主题权限信息
     */
    @Schema(description = "主题权限信息")
    public static class ThemePermissionInfo {
        @Schema(description = "主题ID")
        private Long themeId;
        
        @Schema(description = "主题名称")
        private String themeName;
        
        @Schema(description = "权限级别")
        private String permissionLevel;
        
        @Schema(description = "创建时间")
        private String createdAt;
        
        public ThemePermissionInfo() {}
        
        public ThemePermissionInfo(Long themeId, String themeName, String permissionLevel, String createdAt) {
            this.themeId = themeId;
            this.themeName = themeName;
            this.permissionLevel = permissionLevel;
            this.createdAt = createdAt;
        }
        
        public Long getThemeId() { return themeId; }
        public void setThemeId(Long themeId) { this.themeId = themeId; }
        public String getThemeName() { return themeName; }
        public void setThemeName(String themeName) { this.themeName = themeName; }
        public String getPermissionLevel() { return permissionLevel; }
        public void setPermissionLevel(String permissionLevel) { this.permissionLevel = permissionLevel; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}