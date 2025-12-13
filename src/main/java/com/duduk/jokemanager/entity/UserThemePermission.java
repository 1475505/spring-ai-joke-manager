package com.duduk.jokemanager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_theme_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "theme_id"}))
public class UserThemePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;
    
    @Column(name = "write_permission", nullable = false)
    private Boolean writePermission = false;
    
    @Column(name = "admin_permission", nullable = false)
    private Boolean adminPermission = false;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum PermissionLevel {
        write, admin
    }
    
    // Constructors
    public UserThemePermission() {}
    
    public UserThemePermission(User user, Theme theme, PermissionLevel permissionLevel) {
        this.user = user;
        this.theme = theme;
        setPermissionLevel(permissionLevel);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public void setTheme(Theme theme) {
        this.theme = theme;
    }
    
    public Boolean getWritePermission() {
        return writePermission;
    }
    
    public void setWritePermission(Boolean writePermission) {
        this.writePermission = writePermission;
    }
    
    public Boolean getAdminPermission() {
        return adminPermission;
    }
    
    public void setAdminPermission(Boolean adminPermission) {
        this.adminPermission = adminPermission;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Business methods
    public PermissionLevel getPermissionLevel() {
        if (adminPermission) {
            return PermissionLevel.admin;
        } else if (writePermission) {
            return PermissionLevel.write;
        } else {
            return PermissionLevel.write;
        }
    }
    
    public void setPermissionLevel(PermissionLevel level) {
        this.writePermission = (level == PermissionLevel.write || level == PermissionLevel.admin);
        this.adminPermission = (level == PermissionLevel.admin);
    }
}