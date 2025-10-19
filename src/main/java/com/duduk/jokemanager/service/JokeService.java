package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.JokeRepository;
import com.duduk.jokemanager.repository.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
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
    public Page<Joke> getJokes(Long themeId, Joke.Status status, BigDecimal minScore, 
                              BigDecimal maxScore, String keyword, Boolean isAiGenerated, 
                              Pageable pageable) {
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (themeId != null) {
                Optional<Theme> themeOpt = themeRepository.findById(themeId);
                if (themeOpt.isPresent()) {
                    return jokeRepository.findByThemeAndKeyword(themeOpt.get(), keyword.trim(), pageable);
                }
            } else {
                return jokeRepository.findByKeyword(keyword.trim(), pageable);
            }
        }
        
        if (themeId != null) {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isPresent()) {
                Theme theme = themeOpt.get();
                if (status != null) {
                    return jokeRepository.findByThemeAndStatus(theme, status, pageable);
                }
                if (minScore != null) {
                    return jokeRepository.findByThemeAndFinalScoreGreaterThanEqual(theme, minScore, pageable);
                }
                if (isAiGenerated != null) {
                    return jokeRepository.findByThemeAndIsAiGenerate(theme, isAiGenerated, pageable);
                }
                return jokeRepository.findByTheme(theme, pageable);
            }
        }
        
        if (status != null) {
            return jokeRepository.findByStatus(status, pageable);
        }
        
        if (minScore != null) {
            return jokeRepository.findByFinalScoreGreaterThanEqual(minScore, pageable);
        }
        
        if (isAiGenerated != null) {
            return jokeRepository.findByIsAiGenerate(isAiGenerated, pageable);
        }
        
        return jokeRepository.findAll(pageable);
    }
    
    /**
     * 获取笑话详情
     */
    public Optional<Joke> getJokeById(Long id) {
        return jokeRepository.findById(id);
    }
    
    /**
     * 获取随机笑话
     */
    public List<Joke> getRandomJokes(Long themeId, BigDecimal minScore, int count) {
        if (themeId != null) {
            return jokeRepository.findRandomJokesByTheme(themeId, count);
        } else {
            return jokeRepository.findRandomJokesByMinScore(minScore != null ? minScore : new BigDecimal("0.0"), count);
        }
    }
    
    /**
     * 创建笑话（登录用户）
     */
    public Joke createJoke(String title, String content, Long themeId, User createdBy) {
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
    public Joke createAnonymousJoke(String title, String content, Long themeId) {
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
    public Joke updateJoke(Long id, String title, String content, User updatedBy) {
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
    public void deleteJoke(Long id) {
        jokeRepository.deleteById(id);
    }
    
    /**
     * 设置人工评分
     */
    public Joke setManualScore(Long id, BigDecimal score) {
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
    public Joke changeStatus(Long id, Joke.Status status) {
        return changeStatus(id, status, null);
    }
    
    /**
     * 更改笑话状态（支持拒绝理由）
     */
    public Joke changeStatus(Long id, Joke.Status status, String reason) {
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
     * 增加浏览次数
     */
    public void incrementViewCount(Long id) {
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
    public Joke likeJoke(Long id) {
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
    public Page<Joke> getUserJokes(User user, Joke.Status status, Long themeId, Pageable pageable) {
        if (themeId != null) {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isPresent()) {
                Theme theme = themeOpt.get();
                if (status != null) {
                    return jokeRepository.findByCreatedByAndStatusAndTheme(user, status, theme, pageable);
                }
                return jokeRepository.findByCreatedByAndTheme(user, theme, pageable);
            }
        }
        
        if (status != null) {
            return jokeRepository.findByCreatedByAndStatus(user, status, pageable);
        }
        return jokeRepository.findByCreatedBy(user, pageable);
    }
    
    /**
     * 获取待审核笑话
     */
    public Page<Joke> getPendingJokes(Long themeId, Pageable pageable) {
        Optional<Theme> themeOpt = themeRepository.findById(themeId);
        if (themeOpt.isEmpty()) {
            throw new RuntimeException("主题不存在");
        }
        
        return jokeRepository.findPendingJokesByTheme(themeOpt.get(), pageable);
    }
    
    /**
     * 相似度检测
     */
    public List<Joke> checkSimilarity(String content, Long themeId) {
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
    public double calculateSimilarity(String text1, String text2) {
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