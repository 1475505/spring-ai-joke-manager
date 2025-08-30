package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.UserDto;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.service.UserService;
import com.duduk.jokemanager.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证", description = "用户认证相关接口")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口")
    public ResponseEntity<ApiResponse<UserDto.LoginResponse>> login(@Valid @RequestBody UserDto.LoginRequest request) {
        try {
            Optional<User> userOpt = userService.findByUsername(request.getUsername());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "用户不存在"));
            }
            
            User user = userOpt.get();
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "密码错误"));
            }
            
            // 移除isActive检查，因为User实体中已删除该字段
            
            // 生成JWT令牌
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserName(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUserName());
            
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                    user.getId(), 
                    user.getUserName(), 
                    user.getRole().name(), 
                    user.getAvatarUrl()
            );
            
            UserDto.LoginResponse response = new UserDto.LoginResponse(
                    userInfo, 
                    accessToken, 
                    refreshToken, 
                    3600L
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "登录成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "登录失败：" + e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出接口")
    public ResponseEntity<ApiResponse<String>> logout() {
        // 清除认证信息
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("登出成功"));
    }
    
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册接口，只能注册普通用户")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> register(@Valid @RequestBody UserDto.CreateUserRequest request) {
        try {
            // 强制设置为普通用户角色
            if (!"USER".equals(request.getRole())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "注册时只能创建普通用户"));
            }
            
            // 检查用户名是否已存在
            if (userService.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "用户名已存在"));
            }
            
            User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                User.Role.USER
            );
            
            userService.save(user);
            
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                user.getId(),
                user.getUserName(),
                user.getRole().name(),
                user.getAvatarUrl()
            );
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "注册成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "注册失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的信息")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<User> userOpt = userService.findById(userDetails.getUserId());
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
    
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<UserDto.LoginResponse>> refreshToken(@Valid @RequestBody UserDto.RefreshTokenRequest request) {
        try {
            if (!jwtUtil.validateRefreshToken(request.getRefreshToken())) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "刷新令牌无效或已过期"));
            }
            
            String username = jwtUtil.getUsernameFromToken(request.getRefreshToken());
            Long userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());
            
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User user = userOpt.get();
            // 移除isActive检查，因为User实体中已删除该字段
            
            // 生成新的令牌
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserName(), user.getRole().name());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUserName());
            
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                    user.getId(), 
                    user.getUserName(), 
                    user.getRole().name(), 
                    user.getAvatarUrl()
            );
            
            UserDto.LoginResponse response = new UserDto.LoginResponse(
                    userInfo, 
                    newAccessToken, 
                    newRefreshToken, 
                    3600L
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, "令牌刷新成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "令牌刷新失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody UserDto.ChangePasswordRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<User> userOpt = userService.findById(userDetails.getUserId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User user = userOpt.get();
            
            // 验证旧密码
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "旧密码错误"));
            }
            
            // 更新密码
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userService.save(user);
            
            return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "密码修改失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/profile")
    @Operation(summary = "更新个人信息", description = "更新当前用户的个人信息")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> updateProfile(@Valid @RequestBody UserDto.UpdateProfileRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<User> userOpt = userService.findById(userDetails.getUserId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            User user = userOpt.get();
            
            // 更新个人信息
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            
            userService.save(user);
            
            UserDto.UserInfo userInfo = new UserDto.UserInfo(
                    user.getId(), 
                    user.getUserName(), 
                    user.getRole().name(), 
                    user.getAvatarUrl()
            );
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "个人信息更新成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "个人信息更新失败：" + e.getMessage()));
        }
    }
}