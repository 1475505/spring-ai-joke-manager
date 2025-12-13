package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.ThemeDto;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.entity.UserThemePermission;
import com.duduk.jokemanager.repository.ThemeRepository;
import com.duduk.jokemanager.repository.UserThemePermissionRepository;
import com.duduk.jokemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/themes")
@Tag(name = "主题管理", description = "主题管理相关接口")
public class ThemeController {
    
    @Autowired
    private ThemeRepository themeRepository;
    
    @Autowired
    private UserThemePermissionRepository permissionRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    @Operation(summary = "获取主题列表", description = "获取所有主题列表（公开访问）")
    public ResponseEntity<ApiResponse<List<ThemeDto.ThemeInfo>>> getThemes() {
        try {
            List<Theme> themes = themeRepository.findAll();
            List<ThemeDto.ThemeInfo> themeInfos = themes.stream()
                    .map(theme -> new ThemeDto.ThemeInfo(
                            theme.getId(),
                            theme.getName(),
                            theme.getPrompt(),
                            theme.getIcon(),
                            new ThemeDto.UserInfo(theme.getCreatedBy().getId(), theme.getCreatedBy().getUserName()),
                            theme.getCreatedAt(),
                            theme.getUpdatedAt()
                    ))
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(themeInfos));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取主题列表失败：" + e.getMessage()));
        }
    }
    
    @PostMapping
    @Operation(summary = "创建主题", description = "创建新主题（需要ROOT权限）")
    public ResponseEntity<ApiResponse<ThemeDto.ThemeInfo>> createTheme(@Valid @RequestBody ThemeDto.CreateThemeRequest request) {
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
            if (!user.isRoot()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，只有ROOT用户可以创建主题"));
            }
            
            Theme theme = new Theme(request.getName(), request.getPrompt(), request.getIcon(), user);
            Theme savedTheme = themeRepository.save(theme);
            
            ThemeDto.ThemeInfo themeInfo = new ThemeDto.ThemeInfo(
                    savedTheme.getId(),
                    savedTheme.getName(),
                    savedTheme.getPrompt(),
                    savedTheme.getIcon(),
                    new ThemeDto.UserInfo(user.getId(), user.getUserName()),
                    savedTheme.getCreatedAt(),
                    savedTheme.getUpdatedAt()
            );
            
            return ResponseEntity.ok(ApiResponse.success(themeInfo, "主题创建成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "创建主题失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/{themeId}")
    @Operation(summary = "更新主题", description = "更新主题内容（需要ROOT权限或主题admin权限）")
    public ResponseEntity<ApiResponse<ThemeDto.ThemeInfo>> updateTheme(
            @PathVariable Long themeId,
            @Valid @RequestBody ThemeDto.UpdateThemeRequest request) {
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
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            Theme theme = themeOpt.get();
            
            // 检查权限：root用户或有admin权限的用户
            boolean hasPermission = user.isRoot();
            if (!hasPermission) {
                Optional<UserThemePermission> permissionOpt = permissionRepository.findByUserAndTheme(user, theme);
                hasPermission = permissionOpt.isPresent() && permissionOpt.get().getAdminPermission();
            }
            
            if (!hasPermission) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，只有ROOT用户或有主题admin权限的用户可以更新主题"));
            }
            
            // 更新主题信息
            if (request.getName() != null) {
                theme.setName(request.getName());
            }
            if (request.getPrompt() != null) {
                theme.setPrompt(request.getPrompt());
            }
            if (request.getIcon() != null) {
                theme.setIcon(request.getIcon());
            }
            
            Theme savedTheme = themeRepository.save(theme);
            
            ThemeDto.ThemeInfo themeInfo = new ThemeDto.ThemeInfo(
                    savedTheme.getId(),
                    savedTheme.getName(),
                    savedTheme.getPrompt(),
                    savedTheme.getIcon(),
                    new ThemeDto.UserInfo(savedTheme.getCreatedBy().getId(), savedTheme.getCreatedBy().getUserName()),
                    savedTheme.getCreatedAt(),
                    savedTheme.getUpdatedAt()
            );
            
            return ResponseEntity.ok(ApiResponse.success(themeInfo, "主题更新成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "更新主题失败：" + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{themeId}")
    @Operation(summary = "删除主题", description = "删除主题（需要ROOT权限）")
    public ResponseEntity<ApiResponse<Void>> deleteTheme(@PathVariable Long themeId) {
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
            if (!user.isRoot()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，只有ROOT用户可以删除主题"));
            }
            
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "主题不存在"));
            }
            
            themeRepository.deleteById(themeId);
            
            return ResponseEntity.ok(ApiResponse.success(null, "主题删除成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除主题失败：" + e.getMessage()));
        }
    }

}