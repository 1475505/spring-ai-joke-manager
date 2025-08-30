package com.duduk.jokemanager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "jokes")
public class Joke {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 255)
    private String title;
    
    @Column(nullable = false, length = 2000)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;
    
    // 评分系统
    @Column(name = "ai_score", precision = 3, scale = 1)
    private BigDecimal aiScore = new BigDecimal("7.0");
    
    @Column(name = "manual_score", precision = 3, scale = 1)
    private BigDecimal manualScore;
    
    // 状态管理
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    
    @Column(name = "is_ai_generate", nullable = false)
    private Boolean isAiGenerate = false;
    
    // 统计信息
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
    
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;
    
    // 相似度检测字段
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    private User createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = true)
    private User updatedBy;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // 状态枚举
    public enum Status {
        PENDING, APPROVED, REJECTED, HIDDEN
    }
    
    // 质量等级枚举
    public enum QualityLevel {
        EXCELLENT, GOOD, AVERAGE, POOR
    }
    
    // Constructors
    public Joke() {}
    
    public Joke(String title, String content, Theme theme, User createdBy) {
        this.title = title;
        this.content = content;
        this.theme = theme;
        this.createdBy = createdBy;
        this.updatedBy = createdBy; // 匿名用户时为null
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public void setTheme(Theme theme) {
        this.theme = theme;
    }
    
    public BigDecimal getAiScore() {
        return aiScore;
    }
    
    public void setAiScore(BigDecimal aiScore) {
        this.aiScore = aiScore;
    }
    
    public BigDecimal getManualScore() {
        return manualScore;
    }
    
    public void setManualScore(BigDecimal manualScore) {
        this.manualScore = manualScore;
    }
    
    public BigDecimal getFinalScore() {
        return manualScore != null ? manualScore : (aiScore != null ? aiScore : new BigDecimal("7.0"));
    }
    
    public QualityLevel getQualityLevel() {
        BigDecimal score = getFinalScore();
        double scoreValue = score.doubleValue();
        
        if (scoreValue >= 9.0) {
            return QualityLevel.EXCELLENT;
        } else if (scoreValue >= 8.0) {
            return QualityLevel.GOOD;
        } else if (scoreValue >= 7.0) {
            return QualityLevel.AVERAGE;
        } else {
            return QualityLevel.POOR;
        }
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public Boolean getIsAiGenerate() {
        return isAiGenerate;
    }

    public void setIsAiGenerate(Boolean isAiGenerate) {
        this.isAiGenerate = isAiGenerate;
    }
    
    public Integer getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
    
    public Integer getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public User getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // 业务方法
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }
    
    public void incrementLikeCount() {
        this.likeCount = (this.likeCount == null ? 0 : this.likeCount) + 1;
    }
}