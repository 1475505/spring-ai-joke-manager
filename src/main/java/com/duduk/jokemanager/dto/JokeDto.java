package com.duduk.jokemanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 笑话相关DTO类
 */
public class JokeDto {
    
    /**
     * 笑话信息
     */
    @Schema(description = "笑话信息")
    public static class JokeInfo {
        @Schema(description = "笑话ID", example = "1")
        private Long id;
        
        @Schema(description = "笑话标题", example = "搞笑段子")
        private String title;
        
        @Schema(description = "笑话内容", example = "这是一个很搞笑的段子...")
        private String content;
        
        @Schema(description = "所属主题")
        private ThemeInfo theme;
        
        @Schema(description = "评分信息")
        private ScoreInfo scores;
        
        @Schema(description = "统计信息")
        private StatisticsInfo statistics;
        
        @Schema(description = "状态", example = "APPROVED")
        private String status;
        
        @Schema(description = "是否AI生成", example = "false")
        private Boolean isAiGenerate;
        
        @Schema(description = "作者信息")
        private AuthorInfo author;
        
        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
        
        @Schema(description = "更新时间")
        private LocalDateTime updatedAt;
        
        public JokeInfo() {}
        
        // Constructor with all fields
        public JokeInfo(Long id, String title, String content, ThemeInfo theme, ScoreInfo scores,
                       StatisticsInfo statistics, String status, Boolean isAiGenerate,
                       AuthorInfo author, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.theme = theme;
            this.scores = scores;
            this.statistics = statistics;
            this.status = status;
            this.isAiGenerate = isAiGenerate;
            this.author = author;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public ThemeInfo getTheme() { return theme; }
        public void setTheme(ThemeInfo theme) { this.theme = theme; }
        public ScoreInfo getScores() { return scores; }
        public void setScores(ScoreInfo scores) { this.scores = scores; }
        public StatisticsInfo getStatistics() { return statistics; }
        public void setStatistics(StatisticsInfo statistics) { this.statistics = statistics; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Boolean getIsAiGenerate() { return isAiGenerate; }
        public void setIsAiGenerate(Boolean isAiGenerate) { this.isAiGenerate = isAiGenerate; }
        public AuthorInfo getAuthor() { return author; }
        public void setAuthor(AuthorInfo author) { this.author = author; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
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
        
        @Schema(description = "主题图标", example = "😄")
        private String icon;
        
        public ThemeInfo() {}
        
        public ThemeInfo(Long id, String name, String icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
    
    /**
     * 评分信息
     */
    @Schema(description = "评分信息")
    public static class ScoreInfo {
        @Schema(description = "AI评分", example = "8.2")
        private BigDecimal aiScore;
        
        @Schema(description = "人工评分", example = "8.5")
        private BigDecimal manualScore;
        
        @Schema(description = "最终评分", example = "8.5")
        private BigDecimal finalScore;
        
        public ScoreInfo() {}
        
        public ScoreInfo(BigDecimal aiScore, BigDecimal manualScore, BigDecimal finalScore) {
            this.aiScore = aiScore;
            this.manualScore = manualScore;
            this.finalScore = finalScore;
        }
        
        public BigDecimal getAiScore() { return aiScore; }
        public void setAiScore(BigDecimal aiScore) { this.aiScore = aiScore; }
        public BigDecimal getManualScore() { return manualScore; }
        public void setManualScore(BigDecimal manualScore) { this.manualScore = manualScore; }
        public BigDecimal getFinalScore() { return finalScore; }
        public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    }
    
    /**
     * 统计信息
     */
    @Schema(description = "统计信息")
    public static class StatisticsInfo {
        @Schema(description = "浏览次数", example = "150")
        private Integer viewCount;
        
        @Schema(description = "点赞次数", example = "25")
        private Integer likeCount;
        
        public StatisticsInfo() {}
        
        public StatisticsInfo(Integer viewCount, Integer likeCount) {
            this.viewCount = viewCount;
            this.likeCount = likeCount;
        }
        
        public Integer getViewCount() { return viewCount; }
        public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
        public Integer getLikeCount() { return likeCount; }
        public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    }
    
    /**
     * 作者信息
     */
    @Schema(description = "作者信息")
    public static class AuthorInfo {
        @Schema(description = "作者ID", example = "1")
        private Long id;
        
        @Schema(description = "作者用户名", example = "jokester")
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
     * 创建笑话请求
     */
    @Schema(description = "创建笑话请求")
    public static class CreateJokeRequest {
        @Schema(description = "笑话标题", example = "搞笑段子")
        @Size(max = 100, message = "标题长度不能超过100字符")
        private String title;
        
        @Schema(description = "笑话内容", example = "这是一个很搞笑的段子...", required = true)
        @NotBlank(message = "笑话内容不能为空，请输入至少3个字符的内容")
        @Size(min = 3, max = 2000, message = "笑话内容长度必须在3-2000字符之间。当前内容过短，请添加更多文字使内容更加丰富有趣")
        private String content;
        
        @Schema(description = "主题ID", example = "1", required = true)
        @NotNull(message = "主题ID不能为空")
        private Long themeId;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getThemeId() { return themeId; }
        public void setThemeId(Long themeId) { this.themeId = themeId; }
    }
    
    /**
     * 更新笑话请求
     */
    @Schema(description = "更新笑话请求")
    public static class UpdateJokeRequest {
        @Schema(description = "笑话标题", example = "更新的搞笑段子")
        @Size(max = 100, message = "标题长度不能超过100字符")
        private String title;
        
        @Schema(description = "笑话内容", example = "这是更新后的搞笑段子...")
        @Size(min = 3, max = 2000, message = "笑话内容长度必须在3-2000字符之间")
        private String content;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    /**
     * 设置评分请求
     */
    @Schema(description = "设置评分请求")
    public static class SetScoreRequest {
        @Schema(description = "人工评分", example = "8.5", required = true)
        @NotNull(message = "评分不能为空")
        @DecimalMin(value = "0.0", message = "评分不能小于0")
        @DecimalMax(value = "10.0", message = "评分不能大于10")
        private BigDecimal manualScore;
        
        public BigDecimal getManualScore() { return manualScore; }
        public void setManualScore(BigDecimal manualScore) { this.manualScore = manualScore; }
    }
    
    /**
     * 修改状态请求
     */
    @Schema(description = "修改状态请求")
    public static class ChangeStatusRequest {
        @Schema(description = "状态", example = "APPROVED", required = true, allowableValues = {"PENDING", "APPROVED", "REJECTED", "HIDDEN"})
        @NotBlank(message = "状态不能为空")
        private String status;
        
        @Schema(description = "拒绝理由", example = "内容不符合规范")
        private String reason;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    /**
     * 相似度检测请求
     */
    @Schema(description = "相似度检测请求")
    public static class SimilarityCheckRequest {
        @Schema(description = "笑话内容", example = "这是一个搞笑段子...", required = true)
        @NotBlank(message = "内容不能为空")
        private String content;
        
        @Schema(description = "主题ID", example = "1", required = true)
        @NotNull(message = "主题ID不能为空")
        private Long themeId;
        
        @Schema(description = "相似度阈值", example = "0.95")
        @DecimalMin(value = "0.0", message = "阈值不能小于0")
        @DecimalMax(value = "1.0", message = "阈值不能大于1")
        private BigDecimal threshold = new BigDecimal("0.95");
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getThemeId() { return themeId; }
        public void setThemeId(Long themeId) { this.themeId = themeId; }
        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    }
    
    /**
     * 相似度检测响应
     */
    @Schema(description = "相似度检测响应")
    public static class SimilarityCheckResponse {
        @Schema(description = "是否有相似内容", example = "true")
        private Boolean hasSimilar;
        
        @Schema(description = "最大相似度", example = "0.97")
        private BigDecimal maxSimilarity;
        
        @Schema(description = "相似度阈值", example = "0.95")
        private BigDecimal threshold;
        
        @Schema(description = "相似的笑话列表")
        private List<SimilarJoke> similarJokes;
        
        public SimilarityCheckResponse() {}
        
        public SimilarityCheckResponse(Boolean hasSimilar, BigDecimal maxSimilarity, 
                                     BigDecimal threshold, List<SimilarJoke> similarJokes) {
            this.hasSimilar = hasSimilar;
            this.maxSimilarity = maxSimilarity;
            this.threshold = threshold;
            this.similarJokes = similarJokes;
        }
        
        public Boolean getHasSimilar() { return hasSimilar; }
        public void setHasSimilar(Boolean hasSimilar) { this.hasSimilar = hasSimilar; }
        public BigDecimal getMaxSimilarity() { return maxSimilarity; }
        public void setMaxSimilarity(BigDecimal maxSimilarity) { this.maxSimilarity = maxSimilarity; }
        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
        public List<SimilarJoke> getSimilarJokes() { return similarJokes; }
        public void setSimilarJokes(List<SimilarJoke> similarJokes) { this.similarJokes = similarJokes; }
    }
    
    /**
     * 相似笑话信息
     */
    @Schema(description = "相似笑话信息")
    public static class SimilarJoke {
        @Schema(description = "笑话ID", example = "1")
        private Long id;
        
        @Schema(description = "笑话内容")
        private String content;
        
        @Schema(description = "相似度", example = "0.97")
        private BigDecimal similarity;
        
        @Schema(description = "作者", example = "username")
        private String author;
        
        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
        
        public SimilarJoke() {}
        
        public SimilarJoke(Long id, String content, BigDecimal similarity, String author, LocalDateTime createdAt) {
            this.id = id;
            this.content = content;
            this.similarity = similarity;
            this.author = author;
            this.createdAt = createdAt;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public BigDecimal getSimilarity() { return similarity; }
        public void setSimilarity(BigDecimal similarity) { this.similarity = similarity; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}