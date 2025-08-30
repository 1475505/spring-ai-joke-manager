package com.duduk.jokemanager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_name", unique = true, nullable = false, length = 255)
    private String userName;
    

    
    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.USER;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Comment> comments;
    
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Joke> createdJokes;
    
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Theme> createdThemes;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserThemePermission> themePermissions;
    
    public enum Role {
        ROOT, USER
    }
    
    // Constructors
    public User() {}
    
    public User(String userName, String passwordHash, Role role) {
        this.userName = userName;
        this.passwordHash = passwordHash;
        this.role = role;
    }
    

    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
    
    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }
    
    public List<Joke> getCreatedJokes() {
        return createdJokes;
    }
    
    public void setCreatedJokes(List<Joke> createdJokes) {
        this.createdJokes = createdJokes;
    }
    
    public List<Theme> getCreatedThemes() {
        return createdThemes;
    }
    
    public void setCreatedThemes(List<Theme> createdThemes) {
        this.createdThemes = createdThemes;
    }
    
    public List<UserThemePermission> getThemePermissions() {
        return themePermissions;
    }
    
    public void setThemePermissions(List<UserThemePermission> themePermissions) {
        this.themePermissions = themePermissions;
    }
    
    // Business methods
    public boolean isRoot() {
        return this.role == Role.ROOT;
    }
    
    public boolean hasThemePermission(Long themeId, String permission) {
        if (this.role == Role.ROOT) {
            return true;
        }
        
        return themePermissions.stream()
                .filter(tp -> tp.getTheme().getId().equals(themeId))
                .anyMatch(tp -> {
                    switch (permission.toUpperCase()) {
                        case "READ":
                            return true; // 所有用户都有READ权限
                        case "WRITE":
                        case "CREATE":
                        case "UPDATE":
                        case "DELETE":
                        case "SCORE":
                        case "COMMENT":
                            return tp.getPermissionLevel() == UserThemePermission.PermissionLevel.write ||
                                   tp.getPermissionLevel() == UserThemePermission.PermissionLevel.admin;
                        case "ADMIN":
                            return tp.getPermissionLevel() == UserThemePermission.PermissionLevel.admin;
                        default:
                            return false;
                    }
                });
    }
    
    public boolean hasThemePermission(Long themeId, UserThemePermission.PermissionLevel level) {
        if (this.role == Role.ROOT) {
            return true;
        }
        
        return themePermissions.stream()
                .filter(tp -> tp.getTheme().getId().equals(themeId))
                .anyMatch(tp -> {
                    if (level == UserThemePermission.PermissionLevel.admin) {
                        return tp.getPermissionLevel() == UserThemePermission.PermissionLevel.admin;
                    } else if (level == UserThemePermission.PermissionLevel.write) {
                        return tp.getPermissionLevel() == UserThemePermission.PermissionLevel.write ||
                               tp.getPermissionLevel() == UserThemePermission.PermissionLevel.admin;
                    }
                    return false;
                });
    }
}
