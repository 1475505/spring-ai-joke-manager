package com.duduk.jokemanager.repository;

import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 主题评论相关方法
    Page<Comment> findByTheme(Theme theme, Pageable pageable);
    
    List<Comment> findByThemeOrderByCreatedAtDesc(Theme theme);
    
    @Query("SELECT c FROM Comment c WHERE c.theme = :theme AND " +
           "(c.content LIKE %:keyword% OR c.authorName LIKE %:keyword%)")
    Page<Comment> findByThemeAndKeyword(@Param("theme") Theme theme, 
                                       @Param("keyword") String keyword, 
                                       Pageable pageable);
    
    Long countByTheme(Theme theme);
    
    // 笑话评论相关方法
    Page<Comment> findByJoke(Joke joke, Pageable pageable);
    
    List<Comment> findByJokeOrderByCreatedAtDesc(Joke joke);
    
    @Query("SELECT c FROM Comment c WHERE c.joke = :joke AND " +
           "(c.content LIKE %:keyword% OR c.authorName LIKE %:keyword%)")
    Page<Comment> findByJokeAndKeyword(@Param("joke") Joke joke, 
                                      @Param("keyword") String keyword, 
                                      Pageable pageable);
    
    Long countByJoke(Joke joke);
    
    // 用户评论相关方法
    Page<Comment> findByUser(User user, Pageable pageable);
    
    List<Comment> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT c FROM Comment c WHERE c.user = :user OR c.authorName LIKE %:keyword%")
    Page<Comment> findByAuthor(@Param("user") User user, 
                              @Param("keyword") String keyword, 
                              Pageable pageable);
    
    Long countByUser(User user);
}
