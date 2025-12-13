package com.duduk.jokemanager.repository;

import com.duduk.jokemanager.entity.UserThemePermission;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserThemePermissionRepository extends JpaRepository<UserThemePermission, Long> {
    
    Optional<UserThemePermission> findByUserAndTheme(User user, Theme theme);
    
    List<UserThemePermission> findByUser(User user);
    
    List<UserThemePermission> findByTheme(Theme theme);
    
    // 通过 adminPermission 字段查找管理员权限
    List<UserThemePermission> findByAdminPermissionTrue();
    
    // 通过 writePermission 字段查找写权限
    List<UserThemePermission> findByWritePermissionTrue();
    
    // 查找用户的所有管理员权限主题
    List<UserThemePermission> findByUserAndAdminPermissionTrue(User user);
    
    // 查找用户的所有写权限主题
    List<UserThemePermission> findByUserAndWritePermissionTrue(User user);
    
    boolean existsByUserAndTheme(User user, Theme theme);
    
    void deleteByUserAndTheme(User user, Theme theme);
}