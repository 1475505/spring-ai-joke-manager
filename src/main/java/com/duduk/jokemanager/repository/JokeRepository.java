package com.duduk.jokemanager.repository;

import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JokeRepository extends JpaRepository<Joke, Long> {
    
    // 基础查询
    List<Joke> findByTheme(Theme theme);
    
    Page<Joke> findByTheme(Theme theme, Pageable pageable);
    
    Page<Joke> findByStatus(Joke.Status status, Pageable pageable);
    
    Page<Joke> findByThemeAndStatus(Theme theme, Joke.Status status, Pageable pageable);
    
    List<Joke> findByThemeAndStatus(Theme theme, Joke.Status status);
    
    List<Joke> findByCreatedBy(User createdBy);
    
    Page<Joke> findByCreatedBy(User createdBy, Pageable pageable);
    
    Page<Joke> findByCreatedByAndStatus(User createdBy, Joke.Status status, Pageable pageable);
    
    Page<Joke> findByCreatedByAndTheme(User createdBy, Theme theme, Pageable pageable);
    
    Page<Joke> findByCreatedByAndStatusAndTheme(User createdBy, Joke.Status status, Theme theme, Pageable pageable);
    
    // 评分相关查询
    @Query("SELECT j FROM Joke j WHERE " +
           "(CASE WHEN j.manualScore IS NOT NULL THEN j.manualScore ELSE j.aiScore END) >= :minScore " +
           "ORDER BY (CASE WHEN j.manualScore IS NOT NULL THEN j.manualScore ELSE j.aiScore END) DESC")
    Page<Joke> findByFinalScoreGreaterThanEqual(@Param("minScore") BigDecimal minScore, Pageable pageable);
    
    @Query("SELECT j FROM Joke j WHERE j.theme = :theme AND " +
           "(CASE WHEN j.manualScore IS NOT NULL THEN j.manualScore ELSE j.aiScore END) >= :minScore " +
           "ORDER BY (CASE WHEN j.manualScore IS NOT NULL THEN j.manualScore ELSE j.aiScore END) DESC")
    Page<Joke> findByThemeAndFinalScoreGreaterThanEqual(@Param("theme") Theme theme, 
                                                        @Param("minScore") BigDecimal minScore, 
                                                        Pageable pageable);
    
    // 搜索查询
    @Query("SELECT j FROM Joke j WHERE (j.content LIKE %:keyword% OR j.title LIKE %:keyword%) " +
           "AND j.status = 'APPROVED'")
    Page<Joke> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT j FROM Joke j WHERE j.theme = :theme AND " +
           "(j.content LIKE %:keyword% OR j.title LIKE %:keyword%) AND j.status = 'APPROVED'")
    Page<Joke> findByThemeAndKeyword(@Param("theme") Theme theme, 
                                     @Param("keyword") String keyword, 
                                     Pageable pageable);
    
    // 随机查询
    @Query(value = "SELECT * FROM jokes WHERE theme_id = :themeId AND status = 'APPROVED' " +
                   "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Joke> findRandomJokesByTheme(@Param("themeId") Long themeId, @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM jokes WHERE status = 'APPROVED' AND " +
                   "COALESCE(manual_score, ai_score, 7.0) >= :minScore " +
                   "ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<Joke> findRandomJokesByMinScore(@Param("minScore") BigDecimal minScore, @Param("count") int count);
    
    // AI生成相关查询
    Page<Joke> findByIsAiGenerate(Boolean isAiGenerate, Pageable pageable);
    
    Page<Joke> findByThemeAndIsAiGenerate(Theme theme, Boolean isAiGenerate, Pageable pageable);
    
    // 通过主题ID查询
    List<Joke> findByThemeIdAndStatus(Long themeId, Joke.Status status);
    
    // 统计相关查询
    Long countByTheme(Theme theme);
    
    Long countByThemeAndStatus(Theme theme, Joke.Status status);
    
    Long countByCreatedBy(User createdBy);
    
    Long countByCreatedByAndStatus(User createdBy, Joke.Status status);
    
    // 相似度检测
    @Query("SELECT j FROM Joke j WHERE j.theme = :theme AND j.contentHash = :contentHash")
    List<Joke> findByThemeAndContentHash(@Param("theme") Theme theme, @Param("contentHash") String contentHash);
    
    // 待审核笑话
    @Query("SELECT j FROM Joke j WHERE j.theme = :theme AND j.status = 'PENDING' ORDER BY j.createdAt ASC")
    Page<Joke> findPendingJokesByTheme(@Param("theme") Theme theme, Pageable pageable);
    
    // 获取所有已批准的笑话，包含主题关联
    @Query("SELECT j FROM Joke j JOIN FETCH j.theme WHERE j.status = 'APPROVED'")
    List<Joke> findAllApprovedWithTheme();
}