package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.PageResponse;
import com.duduk.jokemanager.dto.UserDto;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.entity.UserThemePermission;
import com.duduk.jokemanager.service.UserService;
import com.duduk.jokemanager.repository.UserThemePermissionRepository;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
@Tag(name = "用户管理", description = "用户管理相关接口（需要ROOT权限）")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserThemePermissionRepository userThemePermissionRepository;
    
    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.UserInfo>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword) {
        
        try {
            // 参数验证
            if (size > 100) {
                size = 100;
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            // 这里简化实现，实际应该根据参数过滤
            Page<User> users = userService.findAll(pageable);
            
            // 转换为DTO
            Page<UserDto.UserInfo> userInfoPage = users.map(user -> 
                new UserDto.UserInfo(
                    user.getId(),
                    user.getUserName(),
                    user.getRole().name(),
                    user.getAvatarUrl()
                )
            );
            
            PageResponse<UserDto.UserInfo> response = PageResponse.of(userInfoPage);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取用户列表失败：" + e.getMessage()));
        }
    }
    
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> createUser(@Valid @RequestBody UserDto.CreateUserRequest request) {
        try {
            User.Role role = User.Role.valueOf(request.getRole().toUpperCase());
            
            User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                role
            );
            
            userService.save(user);
            
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getRole().name(),
                user.getAvatarUrl()
            );
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "用户创建成功"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的用户角色"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "创建用户失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> getUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User user = userOpt.get();
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getRole().name(),
                user.getAvatarUrl()
            );
            
            return ResponseEntity.ok(ApiResponse.success(userInfo));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取用户信息失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "更新用户信息", description = "更新用户的基本信息")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> updateUser(
            @PathVariable Long userId, 
            @Valid @RequestBody UserDto.UpdateUserRequest request) {
        
        try {
            User.Role role = null;
            if (request.getRole() != null) {
                role = User.Role.valueOf(request.getRole().toUpperCase());
            }
            
            User user = userService.updateUser(userId, role, request.getUsername());
            
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getRole().name(),
                user.getAvatarUrl()
            );
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "用户信息更新成功"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的用户角色"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "更新用户信息失败：" + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success("用户删除成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除用户失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}/password")
    @Operation(summary = "重置用户密码", description = "重置指定用户的密码")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable Long userId,
            @RequestBody ResetPasswordRequest request) {
        
        try {
            userService.resetPassword(userId, request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "密码重置失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/{userId}/permissions")
    @Operation(summary = "获取用户权限列表", description = "获取指定用户的权限信息")
    public ResponseEntity<ApiResponse<UserDto.UserPermissions>> getUserPermissions(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User user = userOpt.get();
            
            // 获取用户的主题权限
            List<UserThemePermission> themePermissions = userThemePermissionRepository.findByUser(user);
            
            // 转换为DTO
            List<UserDto.ThemePermissionInfo> themePermissionInfos = themePermissions.stream()
                .map(permission -> new UserDto.ThemePermissionInfo(
                    permission.getTheme().getId(),
                    permission.getTheme().getName(),
                    permission.getPermissionLevel().name().toLowerCase(),
                    permission.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                .collect(Collectors.toList());
            
            UserDto.UserPermissions userPermissions = new UserDto.UserPermissions(
                user.getId(),
                user.getRole().name(),
                themePermissionInfos
            );
            
            return ResponseEntity.ok(ApiResponse.success(userPermissions));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取用户权限失败：" + e.getMessage()));
        }
    }
    
    /**
     * 重置密码请求
     */
    public static class ResetPasswordRequest {
        private String newPassword;
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}