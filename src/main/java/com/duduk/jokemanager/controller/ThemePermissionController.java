package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.entity.UserThemePermission;
import com.duduk.jokemanager.repository.ThemeRepository;
import com.duduk.jokemanager.repository.UserThemePermissionRepository;
import com.duduk.jokemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/themes/{themeId}/permissions")
@Tag(name = "主题权限管理", description = "主题权限管理相关接口")
public class ThemePermissionController {
    
    @Autowired
    private ThemeRepository themeRepository;
    
    @Autowired
    private UserThemePermissionRepository permissionRepository;
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    @Operation(summary = "赋予用户主题权限", description = "赋予用户主题的write/admin权限（需要ROOT权限或主题admin权限）")
    public ResponseEntity<ApiResponse<String>> grantPermission(
            @PathVariable Long themeId,
            @Valid @RequestBody GrantPermissionRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<User> currentUserOpt = userService.findById(userDetails.getUserId());
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User currentUser = currentUserOpt.get();
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            Theme theme = themeOpt.get();
            
            // 检查当前用户权限：root用户或有admin权限的用户
            boolean hasPermission = currentUser.isRoot();
            if (!hasPermission) {
                Optional<UserThemePermission> permissionOpt = permissionRepository.findByUserAndTheme(currentUser, theme);
                hasPermission = permissionOpt.isPresent() && permissionOpt.get().getAdminPermission();
            }
            
            if (!hasPermission) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，只有ROOT用户或有主题admin权限的用户可以赋予权限"));
            }
            
            // 查找目标用户
            Optional<User> targetUserOpt = userService.findById(request.getUserId());
            if (targetUserOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "目标用户不存在"));
            }
            
            User targetUser = targetUserOpt.get();
            
            // 检查是否已存在权限记录
            Optional<UserThemePermission> existingPermissionOpt = permissionRepository.findByUserAndTheme(targetUser, theme);
            UserThemePermission permission;
            
            if (existingPermissionOpt.isPresent()) {
                permission = existingPermissionOpt.get();
            } else {
                permission = new UserThemePermission();
                permission.setUser(targetUser);
                permission.setTheme(theme);
                permission.setWritePermission(false);
                permission.setAdminPermission(false);
            }
            
            // 设置权限
            if ("write".equals(request.getPermissionType())) {
                permission.setWritePermission(true);
            } else if ("admin".equals(request.getPermissionType())) {
                permission.setAdminPermission(true);
                permission.setWritePermission(true); // admin权限包含write权限
            }
            
            permissionRepository.save(permission);
            
            return ResponseEntity.ok(ApiResponse.success("权限赋予成功", "已成功为用户赋予" + request.getPermissionType() + "权限"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "赋予权限失败：" + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "撤销用户主题权限", description = "撤销用户的主题权限（需要ROOT权限或主题admin权限）")
    public ResponseEntity<ApiResponse<String>> revokePermission(
            @PathVariable Long themeId,
            @PathVariable Long userId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<User> currentUserOpt = userService.findById(userDetails.getUserId());
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User currentUser = currentUserOpt.get();
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            Theme theme = themeOpt.get();
            
            // 检查当前用户权限：root用户或有admin权限的用户
            boolean hasPermission = currentUser.isRoot();
            if (!hasPermission) {
                Optional<UserThemePermission> permissionOpt = permissionRepository.findByUserAndTheme(currentUser, theme);
                hasPermission = permissionOpt.isPresent() && permissionOpt.get().getAdminPermission();
            }
            
            if (!hasPermission) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，只有ROOT用户或有主题admin权限的用户可以撤销权限"));
            }
            
            // 查找目标用户
            Optional<User> targetUserOpt = userService.findById(userId);
            if (targetUserOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "目标用户不存在"));
            }
            
            User targetUser = targetUserOpt.get();
            
            // 删除权限记录
            permissionRepository.deleteByUserAndTheme(targetUser, theme);
            
            return ResponseEntity.ok(ApiResponse.success("权限撤销成功", "已成功撤销用户的主题权限"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "撤销权限失败：" + e.getMessage()));
        }
    }
    
    @Schema(description = "赋予权限请求")
    public static class GrantPermissionRequest {
        @Schema(description = "用户ID", example = "2", required = true)
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        
        @Schema(description = "权限类型", example = "write", allowableValues = {"write", "admin"}, required = true)
        @NotNull(message = "权限类型不能为空")
        private String permissionType;
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getPermissionType() {
            return permissionType;
        }
        
        public void setPermissionType(String permissionType) {
            this.permissionType = permissionType;
        }
    }
}