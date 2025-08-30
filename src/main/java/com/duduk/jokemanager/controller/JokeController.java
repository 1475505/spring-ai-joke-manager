package com.duduk.jokemanager.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duduk.jokemanager.config.JwtAuthenticationFilter;
import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.dto.JokeDto;
import com.duduk.jokemanager.dto.PageResponse;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.entity.UserThemePermission;
import com.duduk.jokemanager.service.JokeService;
import com.duduk.jokemanager.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/jokes")
@Tag(name = "笑话管理", description = "笑话管理相关接口")
public class JokeController {

    @Autowired
    private JokeService jokeService;
    
    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "获取笑话列表", description = "分页获取笑话列表（公开访问）")
    public ResponseEntity<ApiResponse<PageResponse<JokeDto.JokeInfo>>> getJokes(
            @RequestParam(required = false) Long themeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "finalScore") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minScore,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isAiGenerate) {
        
        try {
            if (size > 100) size = 100;
            
            Sort sortObj = Sort.by("desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, 
                                 getSortField(sort));
            Pageable pageable = PageRequest.of(page, size, sortObj);
            
            Joke.Status statusEnum = Joke.Status.APPROVED; // 默认只显示已审核通过的笑话
            if (status != null) {
                try {
                    statusEnum = Joke.Status.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error(400, "无效的状态值"));
                }
            }
            
            Page<Joke> jokes = jokeService.getJokes(themeId, statusEnum, minScore, maxScore, 
                                                   keyword, isAiGenerate, pageable);
            
            Page<JokeDto.JokeInfo> jokeInfoPage = jokes.map(this::convertToJokeInfo);
            PageResponse<JokeDto.JokeInfo> response = PageResponse.of(jokeInfoPage);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取笑话列表失败：" + e.getMessage()));
        }
    }

    @GetMapping("/{jokeId}")
    @Operation(summary = "获取笑话详情", description = "根据笑话ID获取详细信息")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> getJoke(@PathVariable Long jokeId) {
        try {
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(jokeOpt.get());
            return ResponseEntity.ok(ApiResponse.success(jokeInfo));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取笑话详情失败：" + e.getMessage()));
        }
    }

    @GetMapping("/random")
    @Operation(summary = "随机获取笑话", description = "随机获取指定数量的笑话")
    public ResponseEntity<ApiResponse<List<JokeDto.JokeInfo>>> getRandomJokes(
            @RequestParam(required = false) Long themeId,
            @RequestParam(required = false) BigDecimal minScore,
            @RequestParam(defaultValue = "false") Boolean excludeViewed,
            @RequestParam(defaultValue = "1") int count) {
        
        try {
            if (count > 10) count = 10;
            
            List<Joke> jokes = jokeService.getRandomJokes(themeId, minScore, count);
            List<JokeDto.JokeInfo> jokeInfos = jokes.stream()
                    .map(this::convertToJokeInfo)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(jokeInfos));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取随机笑话失败：" + e.getMessage()));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "搜索笑话", description = "根据关键词搜索笑话")
    public ResponseEntity<ApiResponse<PageResponse<JokeDto.JokeInfo>>> searchJokes(
            @RequestParam String keyword,
            @RequestParam(required = false) Long themeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BigDecimal minScore) {
        
        try {
            if (size > 100) size = 100;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Joke> jokes = jokeService.getJokes(themeId, Joke.Status.APPROVED, minScore, 
                                                   null, keyword, null, pageable);
            
            Page<JokeDto.JokeInfo> jokeInfoPage = jokes.map(this::convertToJokeInfo);
            PageResponse<JokeDto.JokeInfo> response = PageResponse.of(jokeInfoPage);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "搜索笑话失败：" + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "投稿笑话", description = "用户投稿新笑话（支持匿名投稿）")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> createJoke(@Valid @RequestBody JokeDto.CreateJokeRequest request) {
        try {
            // 处理标题默认值
            String title = request.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "匿名用户的笑话";
            }
            
            Joke joke;
            
            // 检查是否登录
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails) {
                // 登录用户投稿
                JwtAuthenticationFilter.JwtUserDetails userDetails = 
                        (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
                
                Optional<User> userOpt = userService.findById(userDetails.getUserId());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    joke = jokeService.createJoke(title, request.getContent(), 
                                                 request.getThemeId(), user);
                } else {
                    // 用户不存在，按匿名处理
                    joke = jokeService.createAnonymousJoke(title, request.getContent(), 
                                                          request.getThemeId());
                }
            } else {
                // 匿名用户投稿
                joke = jokeService.createAnonymousJoke(title, request.getContent(), 
                                                      request.getThemeId());
            }
            
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(joke);
            return ResponseEntity.ok(ApiResponse.success(jokeInfo, "笑话投稿成功，等待审核"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "投稿笑话失败：" + e.getMessage()));
        }
    }

    @PostMapping("/similarity-check")
    @Operation(summary = "检查内容相似度", description = "检查笑话内容的相似度")
    public ResponseEntity<ApiResponse<JokeDto.SimilarityCheckResponse>> checkSimilarity(
            @Valid @RequestBody JokeDto.SimilarityCheckRequest request) {
        
        try {
            List<Joke> similarJokes = jokeService.checkSimilarity(request.getContent(), request.getThemeId());
            
            List<JokeDto.SimilarJoke> similarJokeList = similarJokes.stream()
                    .map(joke -> {
                        double similarity = jokeService.calculateSimilarity(request.getContent(), joke.getContent());
                        return new JokeDto.SimilarJoke(
                                joke.getId(),
                                joke.getContent(),
                                BigDecimal.valueOf(similarity),
                                joke.getCreatedBy().getUserName(),
                                joke.getCreatedAt()
                        );
                    })
                    .filter(similar -> similar.getSimilarity().compareTo(request.getThreshold()) > 0)
                    .collect(Collectors.toList());
            
            BigDecimal maxSimilarity = similarJokeList.stream()
                    .map(JokeDto.SimilarJoke::getSimilarity)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            boolean hasSimilar = maxSimilarity.compareTo(request.getThreshold()) > 0;
            
            JokeDto.SimilarityCheckResponse response = new JokeDto.SimilarityCheckResponse(
                    hasSimilar, maxSimilarity, request.getThreshold(), similarJokeList
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "相似度检测失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{jokeId}/like")
    @Operation(summary = "点赞笑话", description = "对笑话进行点赞")
    public ResponseEntity<ApiResponse<String>> likeJoke(@PathVariable Long jokeId) {
        try {
            jokeService.likeJoke(jokeId);
            return ResponseEntity.ok(ApiResponse.success("点赞成功"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "点赞失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{jokeId}/view")
    @Operation(summary = "增加浏览次数", description = "增加笑话的浏览次数")
    public ResponseEntity<ApiResponse<String>> incrementView(@PathVariable Long jokeId) {
        try {
            jokeService.incrementViewCount(jokeId);
            return ResponseEntity.ok(ApiResponse.success("浏览次数已更新"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "更新浏览次数失败：" + e.getMessage()));
        }
    }

    // ==================== 用户个人笑话管理 ====================
    
    @GetMapping("/my")
    @Operation(summary = "获取我的投稿列表", description = "获取当前用户的投稿笑话列表")
    public ResponseEntity<ApiResponse<PageResponse<JokeDto.JokeInfo>>> getMyJokes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long themeId) {
        
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
            
            Joke.Status statusEnum = null;
            if (status != null) {
                try {
                    statusEnum = Joke.Status.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error(400, "无效的状态值"));
                }
            }
            
            if (size > 100) size = 100;
            
            Sort sortObj = Sort.by("desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, 
                                 getSortField(sort));
            Pageable pageable = PageRequest.of(page, size, sortObj);
            
            Page<Joke> jokes = jokeService.getUserJokes(user, statusEnum, themeId, pageable);
            Page<JokeDto.JokeInfo> jokeInfoPage = jokes.map(this::convertToJokeInfo);
            PageResponse<JokeDto.JokeInfo> response = PageResponse.of(jokeInfoPage);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取投稿列表失败：" + e.getMessage()));
        }
    }
    
    @GetMapping("/my/{jokeId}")
    @Operation(summary = "获取我的投稿详情", description = "获取当前用户的指定投稿详情")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> getMyJoke(@PathVariable Long jokeId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            Joke joke = jokeOpt.get();
            // 检查是否为匿名投稿或者不是当前用户的投稿
            if (joke.getCreatedBy() == null || !joke.getCreatedBy().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "只能查看自己的投稿"));
            }
            
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(joke);
            return ResponseEntity.ok(ApiResponse.success(jokeInfo));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取投稿详情失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/my/{jokeId}")
    @Operation(summary = "修改我的投稿", description = "修改当前用户的投稿（仅待审核状态）")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> updateMyJoke(
            @PathVariable Long jokeId, 
            @Valid @RequestBody JokeDto.UpdateJokeRequest request) {
        
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
            
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            Joke joke = jokeOpt.get();
            
            // 权限检查：ROOT用户可以修改任何笑话，主题管理员可以修改其管理主题下的笑话，普通用户只能修改自己待审核的笑话
            boolean canUpdate = false;
            
            if (user.isRoot()) {
                // ROOT用户可以修改任何笑话
                canUpdate = true;
            } else if (user.hasThemePermission(joke.getTheme().getId(), UserThemePermission.PermissionLevel.write)) {
                // 主题管理员可以修改其管理主题下的笑话
                canUpdate = true;
            } else if (joke.getCreatedBy().getId().equals(user.getId()) && joke.getStatus() == Joke.Status.PENDING) {
                // 普通用户只能修改自己创建的待审核笑话
                canUpdate = true;
            }
            
            if (!canUpdate) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "没有权限修改此笑话"));
            }
            
            Joke updatedJoke = jokeService.updateJoke(jokeId, request.getTitle(), request.getContent(), user);
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(updatedJoke);
            
            return ResponseEntity.ok(ApiResponse.success(jokeInfo, "投稿修改成功"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "修改投稿失败：" + e.getMessage()));
        }
    }
    
    @DeleteMapping("/my/{jokeId}")
    @Operation(summary = "删除我的投稿", description = "删除当前用户的投稿（仅待审核状态）")
    public ResponseEntity<ApiResponse<String>> deleteMyJoke(@PathVariable Long jokeId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getDetails() instanceof JwtAuthenticationFilter.JwtUserDetails)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "未登录"));
            }
            
            JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (JwtAuthenticationFilter.JwtUserDetails) auth.getDetails();
            
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            Joke joke = jokeOpt.get();
            if (!joke.getCreatedBy().getId().equals(userDetails.getUserId())) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "只能删除自己的投稿"));
            }
            
            if (joke.getStatus() != Joke.Status.PENDING) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "只能删除待审核状态的投稿"));
            }
            
            jokeService.deleteJoke(jokeId);
            return ResponseEntity.ok(ApiResponse.success("投稿删除成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除投稿失败：" + e.getMessage()));
        }
    }
    
    // ==================== 管理员审核功能 ====================
    
    @GetMapping("/themes/{themeId}/pending")
    @Operation(summary = "获取待审核笑话列表", description = "获取指定主题的待审核笑话列表（需要主题管理权限）")
    public ResponseEntity<ApiResponse<PageResponse<JokeDto.JokeInfo>>> getPendingJokes(
            @PathVariable Long themeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "asc") String order) {
        
        try {
            // 权限检查：需要登录且有主题管理权限
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
            if (!user.isRoot() && !user.hasThemePermission(themeId, UserThemePermission.PermissionLevel.write)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，需要主题管理权限"));
            }
            
            if (size > 100) size = 100;
            Sort sortObj = Sort.by("desc".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC, sort);
            Pageable pageable = PageRequest.of(page, size, sortObj);
            
            Page<Joke> jokes = jokeService.getPendingJokes(themeId, pageable);
            Page<JokeDto.JokeInfo> jokeInfoPage = jokes.map(this::convertToJokeInfo);
            PageResponse<JokeDto.JokeInfo> response = PageResponse.of(jokeInfoPage);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "获取待审核笑话失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/{jokeId}/approve")
    @Operation(summary = "审批通过", description = "审批通过笑话（需要主题管理权限）")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> approveJoke(
            @PathVariable Long jokeId,
            @RequestBody(required = false) JokeDto.SetScoreRequest scoreRequest) {
        
        try {
            // 权限检查：需要登录且有主题管理权限
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
            
            // 获取笑话信息以检查主题权限
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            User user = userOpt.get();
            Joke joke = jokeOpt.get();
            if (!user.isRoot() && !user.hasThemePermission(joke.getTheme().getId(), UserThemePermission.PermissionLevel.write)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，需要主题写入权限"));
            }
            
            joke = jokeService.changeStatus(jokeId, Joke.Status.APPROVED);
            
            if (scoreRequest != null && scoreRequest.getManualScore() != null) {
                joke = jokeService.setManualScore(jokeId, scoreRequest.getManualScore());
            }
            
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(joke);
            return ResponseEntity.ok(ApiResponse.success(jokeInfo, "笑话审批通过"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "审批失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/{jokeId}/score")
    @Operation(summary = "设置人工评分", description = "设置笑话的人工评分（需要主题管理权限）")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> setScore(
            @PathVariable Long jokeId,
            @Valid @RequestBody JokeDto.SetScoreRequest request) {
        
        try {
            // 权限检查：需要登录且有主题管理权限
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
            
            // 获取笑话信息以检查主题权限
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            User user = userOpt.get();
            Joke joke = jokeOpt.get();
            if (!user.isRoot() && !user.hasThemePermission(joke.getTheme().getId(), UserThemePermission.PermissionLevel.write)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，需要主题写入权限"));
            }
            
            joke = jokeService.setManualScore(jokeId, request.getManualScore());
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(joke);
            
            return ResponseEntity.ok(ApiResponse.success(jokeInfo, "评分设置成功"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "设置评分失败：" + e.getMessage()));
        }
    }
    
    @PutMapping("/{jokeId}/status")
    @Operation(summary = "修改笑话状态", description = "修改笑话状态（需要主题管理权限）")
    public ResponseEntity<ApiResponse<JokeDto.JokeInfo>> changeStatus(
            @PathVariable Long jokeId,
            @Valid @RequestBody JokeDto.ChangeStatusRequest request) {
        
        try {
            // 权限检查：需要登录且有主题管理权限
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
            
            // 获取笑话信息以检查主题权限
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            User user = userOpt.get();
            Joke joke = jokeOpt.get();
            if (!user.isRoot() && !user.hasThemePermission(joke.getTheme().getId(), UserThemePermission.PermissionLevel.write)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，需要主题写入权限"));
            }
            
            Joke.Status status;
            try {
                status = Joke.Status.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "无效的状态值"));
            }
            
            Joke updatedJoke = jokeService.changeStatus(jokeId, status, request.getReason());
            JokeDto.JokeInfo jokeInfo = convertToJokeInfo(updatedJoke);
            
            return ResponseEntity.ok(ApiResponse.success(jokeInfo, "状态修改成功"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "修改状态失败：" + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{jokeId}")
    @Operation(summary = "删除笑话", description = "删除指定笑话（需要登录）")
    public ResponseEntity<ApiResponse<String>> deleteJoke(@PathVariable Long jokeId) {
        try {
            // 权限检查：需要登录且有主题写入权限
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
            
            // 获取笑话信息以检查主题权限
            Optional<Joke> jokeOpt = jokeService.getJokeById(jokeId);
            if (jokeOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(404, "笑话不存在"));
            }
            
            User user = userOpt.get();
            Joke joke = jokeOpt.get();
            if (!user.isRoot() && !user.hasThemePermission(joke.getTheme().getId(), UserThemePermission.PermissionLevel.write)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，需要主题写入权限"));
            }
            
            jokeService.deleteJoke(jokeId);
            return ResponseEntity.ok(ApiResponse.success("笑话删除成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "删除笑话失败：" + e.getMessage()));
        }
    }

    // 私有辅助方法
    private JokeDto.JokeInfo convertToJokeInfo(Joke joke) {
        JokeDto.ThemeInfo themeInfo = new JokeDto.ThemeInfo(
                joke.getTheme().getId(),
                joke.getTheme().getName(),
                joke.getTheme().getIcon()
        );
        
        JokeDto.ScoreInfo scoreInfo = new JokeDto.ScoreInfo(
                joke.getAiScore(),
                joke.getManualScore(),
                joke.getFinalScore(),
                joke.getQualityLevel().name()
        );
        
        JokeDto.StatisticsInfo statisticsInfo = new JokeDto.StatisticsInfo(
                joke.getViewCount(),
                joke.getLikeCount()
        );
        
        // 处理匿名用户情况
        JokeDto.AuthorInfo authorInfo;
        String title;
        if (joke.getCreatedBy() != null) {
            // 有作者信息
            authorInfo = new JokeDto.AuthorInfo(
                    joke.getCreatedBy().getId(),
                    joke.getCreatedBy().getUserName(),
                    joke.getCreatedBy().getAvatarUrl()
            );
            title = joke.getTitle() != null && !joke.getTitle().trim().isEmpty() ? 
                    joke.getTitle() : joke.getCreatedBy().getUserName() + "的笑话";
        } else {
            // 匿名用户
            authorInfo = new JokeDto.AuthorInfo(
                    null,
                    "匿名用户",
                    null
            );
            title = joke.getTitle() != null && !joke.getTitle().trim().isEmpty() ? 
                    joke.getTitle() : "匿名用户的笑话";
        }
        
        return new JokeDto.JokeInfo(
                joke.getId(),
                title,
                joke.getContent(),
                themeInfo,
                scoreInfo,
                statisticsInfo,
                joke.getStatus().name(),
                joke.getIsAiGenerate(),
                authorInfo,
                joke.getCreatedAt(),
                joke.getUpdatedAt()
        );
    }
    
    private String getSortField(String sort) {
        switch (sort) {
            case "finalScore":
                return "manualScore"; // 实际需要自定义排序逻辑
            case "createdAt":
                return "createdAt";
            case "likeCount":
                return "likeCount";
            case "viewCount":
                return "viewCount";
            default:
                return "createdAt";
        }
    }
}