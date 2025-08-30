package com.duduk.jokemanager.repository;

import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    
    Optional<Theme> findByName(String name);
    
    List<Theme> findByCreatedBy(User createdBy);
    
    List<Theme> findByCreatedByOrderByCreatedAtDesc(User createdBy);
    
    @Query("SELECT t FROM Theme t ORDER BY t.createdAt DESC")
    List<Theme> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT t FROM Theme t WHERE t.name LIKE %:keyword% OR t.description LIKE %:keyword%")
    List<Theme> findByKeyword(@Param("keyword") String keyword);
    
    boolean existsByName(String name);
}