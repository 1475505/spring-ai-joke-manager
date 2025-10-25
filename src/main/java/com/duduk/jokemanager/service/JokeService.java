package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.JokeRepository;
import com.duduk.jokemanager.repository.ThemeRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JokeService {
    
    @Autowired
    private JokeRepository jokeRepository;
    
    @Autowired
    private ThemeRepository themeRepository;
    
    /**
     * 获取笑话列表
     */
    @Tool(description = "获取笑话列表，支持按主题、状态、评分、关键词等条件筛选")
    public Map<String, Object> getJokes(
            @ToolParam(description = "主题ID，可选参数") Long themeId, 
            @ToolParam(description = "笑话状态，可选值：PENDING, APPROVED, REJECTED, HIDDEN") Joke.Status status, 
            @ToolParam(description = "最低评分，可选参数") BigDecimal minScore, 
            @ToolParam(description = "最高评分，可选参数") BigDecimal maxScore, 
            @ToolParam(description = "关键词搜索，可选参数") String keyword, 
            @ToolParam(description = "是否AI生成，可选参数") Boolean isAiGenerated, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        
        Page<Joke> pageResult;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (themeId != null) {
                Optional<Theme> themeOpt = themeRepository.findById(themeId);
                if (themeOpt.isPresent()) {
                    pageResult = jokeRepository.findByThemeAndKeyword(themeOpt.get(), keyword.trim(), pageable);
                } else {
                    pageResult = jokeRepository.findByKeyword(keyword.trim(), pageable);
                }
            } else {
                pageResult = jokeRepository.findByKeyword(keyword.trim(), pageable);
            }
        } else if (themeId != null) {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isPresent()) {
                Theme theme = themeOpt.get();
                if (status != null) {
                    pageResult = jokeRepository.findByThemeAndStatus(theme, status, pageable);
                } else if (minScore != null) {
                    pageResult = jokeRepository.findByThemeAndFinalScoreGreaterThanEqual(theme, minScore, pageable);
                } else if (isAiGenerated != null) {
                    pageResult = jokeRepository.findByThemeAndIsAiGenerate(theme, isAiGenerated, pageable);
                } else {
                    pageResult = jokeRepository.findByTheme(theme, pageable);
                }
            } else {
                pageResult = Page.empty(pageable);
            }
        } else if (status != null) {
            pageResult = jokeRepository.findByStatus(status, pageable);
        } else if (minScore != null) {
            pageResult = jokeRepository.findByFinalScoreGreaterThanEqual(minScore, pageable);
        } else if (isAiGenerated != null) {
            pageResult = jokeRepository.findByIsAiGenerate(isAiGenerated, pageable);
        } else {
            pageResult = jokeRepository.findAll(pageable);
        }
        
        // 转换为Map格式，避免Spring AI @Tool注解的函数式类型警告
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageResult.getContent());
        result.put("page", pageResult.getNumber());
        result.put("size", pageResult.getSize());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("first", pageResult.isFirst());
        result.put("last", pageResult.isLast());
        result.put("empty", pageResult.isEmpty());
        
        return result;
    }
    
    /**
     * 获取笑话详情
     */
    @Tool(description = "根据ID获取笑话详情，包括标题、内容、主题、评分等信息")
    public Optional<Joke> getJokeById(@ToolParam(description = "笑话的ID") Long id) {
        return jokeRepository.findById(id);
    }
    
    /**
     * 获取随机笑话
     */
    @Tool(description = "获取随机笑话，可以指定主题ID、最低评分和数量")
    public List<Joke> getRandomJokes(
            @ToolParam(description = "主题ID，可选参数，不指定则从所有主题中获取") Long themeId, 
            @ToolParam(description = "最低评分，可选参数，默认为0.0") BigDecimal minScore, 
            @ToolParam(description = "获取的笑话数量，默认为10") int count) {
        if (themeId != null) {
            return jokeRepository.findRandomJokesByTheme(themeId, count);
        } else {
            return jokeRepository.findRandomJokesByMinScore(minScore != null ? minScore : new BigDecimal("0.0"), count);
        }
    }
    
    /**
     * 创建笑话（登录用户）
     */
    @Tool(description = "创建新笑话，需要提供标题、内容、主题ID和创建者用户信息")
    public Joke createJoke(
            @ToolParam(description = "笑话的标题") String title, 
            @ToolParam(description = "笑话的内容") String content, 
            @ToolParam(description = "主题ID") Long themeId, 
            @ToolParam(description = "创建笑话的用户") User createdBy) {
        Optional<Theme> themeOpt = themeRepository.findById(themeId);
        if (themeOpt.isEmpty()) {
            throw new RuntimeException("主题不存在");
        }
        
        Theme theme = themeOpt.get();
        Joke joke = new Joke(title, content, theme, createdBy);
        
        return jokeRepository.save(joke);
    }

    /**
     * 创建笑话（匿名用户）
     */
    @Tool(description = "创建匿名笑话，不需要用户登录")
    public Joke createAnonymousJoke(
            @ToolParam(description = "笑话标题") String title, 
            @ToolParam(description = "笑话内容") String content, 
            @ToolParam(description = "主题ID") Long themeId) {
        Optional<Theme> themeOpt = themeRepository.findById(themeId);
        if (themeOpt.isEmpty()) {
            throw new RuntimeException("主题不存在");
        }
        
        Theme theme = themeOpt.get();
        Joke joke = new Joke(title, content, theme, null); // 匿名用户传null
        
        return jokeRepository.save(joke);
    }

    /**
     * 更新笑话
     */
    @Tool(description = "更新已有笑话的标题和内容")
    public Joke updateJoke(
            @ToolParam(description = "要更新的笑话ID") Long id, 
            @ToolParam(description = "新的笑话标题，可选参数") String title, 
            @ToolParam(description = "新的笑话内容，可选参数") String content, 
            @ToolParam(description = "执行更新的用户") User updatedBy) {
        Optional<Joke> jokeOpt = jokeRepository.findById(id);
        if (jokeOpt.isEmpty()) {
            throw new RuntimeException("笑话不存在");
        }
        
        Joke joke = jokeOpt.get();
        
        if (title != null) {
            joke.setTitle(title);
        }
        if (content != null) {
            joke.setContent(content);
        }
        joke.setUpdatedBy(updatedBy);
        
        return jokeRepository.save(joke);
    }
    
    /**
     * 删除笑话
     */
    @Tool(description = "根据ID删除指定的笑话")
    public void deleteJoke(@ToolParam(description = "要删除的笑话ID") Long id) {
        jokeRepository.deleteById(id);
    }
    
    /**
     * 设置人工评分
     */
    @Tool(description = "为笑话设置人工评分")
    public Joke setManualScore(
            @ToolParam(description = "笑话ID") Long id, 
            @ToolParam(description = "人工评分值") BigDecimal score) {
        Optional<Joke> jokeOpt = jokeRepository.findById(id);
        if (jokeOpt.isEmpty()) {
            throw new RuntimeException("笑话不存在");
        }
        
        Joke joke = jokeOpt.get();
        joke.setManualScore(score);
        
        return jokeRepository.save(joke);
    }
    
    /**
     * 更改笑话状态
     */
    @Tool(description = "更改笑话的状态，如审核通过、拒绝、隐藏等")
    public Joke changeJokeStatus(
            @ToolParam(description = "要更改状态的笑话ID") Long id, 
            @ToolParam(description = "新的笑话状态，可选值：PENDING, APPROVED, REJECTED, HIDDEN") Joke.Status status) {
        return changeJokeStatusWithReason(id, status, null);
    }
    
    /**
     * 更改笑话状态（支持拒绝理由）
     */
    @Tool(description = "更改笑话的状态，支持添加拒绝理由")
    public Joke changeJokeStatusWithReason(
            @ToolParam(description = "要更改状态的笑话ID") Long id, 
            @ToolParam(description = "新的笑话状态，可选值：PENDING, APPROVED, REJECTED, HIDDEN") Joke.Status status, 
            @ToolParam(description = "拒绝理由，仅在状态为REJECTED时使用") String reason) {
        Optional<Joke> jokeOpt = jokeRepository.findById(id);
        if (jokeOpt.isEmpty()) {
            throw new RuntimeException("笑话不存在");
        }
        
        Joke joke = jokeOpt.get();
        
        // 如果是拒绝状态且提供了理由，则在内容前添加拒绝理由
        if (status == Joke.Status.REJECTED) {
            String rejectReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "无";
            String rejectPrefix = "【admin拒绝，理由：" + rejectReason + "。请修改后重新投稿】\n\n";
            
            // 检查内容是否已经包含拒绝理由前缀，避免重复添加
            if (!joke.getContent().startsWith("【admin拒绝，理由：")) {
                joke.setContent(rejectPrefix + joke.getContent());
            }
        }
        
        joke.setStatus(status);
        
        return jokeRepository.save(joke);
    }
    
    /**
     * 内部方法：更改笑话状态（支持拒绝理由）
     */
    public Joke changeStatus(Long id, Joke.Status status, String reason) {
        return changeJokeStatusWithReason(id, status, reason);
    }
    
    /**
     * 增加浏览次数
     */
    public void incrementViewCount(@ToolParam(description = "笑话ID") Long id) {
        Optional<Joke> jokeOpt = jokeRepository.findById(id);
        if (jokeOpt.isPresent()) {
            Joke joke = jokeOpt.get();
            joke.incrementViewCount();
            jokeRepository.save(joke);
        }
    }
    
    /**
     * 点赞笑话
     */
    public Joke likeJoke(@ToolParam(description = "笑话ID") Long id) {
        Optional<Joke> jokeOpt = jokeRepository.findById(id);
        if (jokeOpt.isEmpty()) {
            throw new RuntimeException("笑话不存在");
        }
        
        Joke joke = jokeOpt.get();
        joke.incrementLikeCount();
        
        return jokeRepository.save(joke);
    }
    
    /**
     * 获取用户的投稿
     */
    @Tool(description = "获取指定用户的投稿笑话列表")
    public Map<String, Object> getUserJokes(
            @ToolParam(description = "用户对象") User user, 
            @ToolParam(description = "笑话状态，可选值：PENDING, APPROVED, REJECTED, HIDDEN") Joke.Status status, 
            @ToolParam(description = "主题ID，可选参数") Long themeId, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        
        Page<Joke> pageResult;
        
        if (themeId != null) {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isPresent()) {
                Theme theme = themeOpt.get();
                if (status != null) {
                    pageResult = jokeRepository.findByCreatedByAndStatusAndTheme(user, status, theme, pageable);
                } else {
                    pageResult = jokeRepository.findByCreatedByAndTheme(user, theme, pageable);
                }
            } else {
                pageResult = Page.empty(pageable);
            }
        } else if (status != null) {
            pageResult = jokeRepository.findByCreatedByAndStatus(user, status, pageable);
        } else {
            pageResult = jokeRepository.findByCreatedBy(user, pageable);
        }
        
        // 转换为Map格式，避免Spring AI @Tool注解的函数式类型警告
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageResult.getContent());
        result.put("page", pageResult.getNumber());
        result.put("size", pageResult.getSize());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("first", pageResult.isFirst());
        result.put("last", pageResult.isLast());
        result.put("empty", pageResult.isEmpty());
        
        return result;
    }
    
    /**
     * 获取待审核笑话
     */
    @Tool(description = "获取指定主题下待审核的笑话列表")
    public Map<String, Object> getPendingJokes(
            @ToolParam(description = "主题ID") Long themeId, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        Optional<Theme> themeOpt = themeRepository.findById(themeId);
        if (themeOpt.isEmpty()) {
            throw new RuntimeException("主题不存在");
        }
        
        Page<Joke> pageResult = jokeRepository.findPendingJokesByTheme(themeOpt.get(), pageable);
        
        // 转换为Map格式，避免Spring AI @Tool注解的函数式类型警告
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageResult.getContent());
        result.put("page", pageResult.getNumber());
        result.put("size", pageResult.getSize());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("first", pageResult.isFirst());
        result.put("last", pageResult.isLast());
        result.put("empty", pageResult.isEmpty());
        
        return result;
    }
    
    /**
     * 相似度检测
     */
    public List<Joke> checkSimilarity(
            @ToolParam(description = "要检测的笑话内容") String content, 
            @ToolParam(description = "主题ID") Long themeId) {
        Optional<Theme> themeOpt = themeRepository.findById(themeId);
        if (themeOpt.isEmpty()) {
            throw new RuntimeException("主题不存在");
        }
        
        // 只获取同主题下状态为APPROVED的笑话进行相似度比较
        List<Joke> approvedJokesInTheme = jokeRepository.findByThemeAndStatus(themeOpt.get(), Joke.Status.APPROVED);
        return approvedJokesInTheme;
    }
    
    /**
     * 简单的相似度计算（基于编辑距离）
     */
    public double calculateSimilarity(
            @ToolParam(description = "第一段文本") String text1, 
            @ToolParam(description = "第二段文本") String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        text1 = text1.toLowerCase().trim();
        text2 = text2.toLowerCase().trim();
        
        if (text1.equals(text2)) return 1.0;
        
        int maxLength = Math.max(text1.length(), text2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(text1, text2);
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * 计算编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}