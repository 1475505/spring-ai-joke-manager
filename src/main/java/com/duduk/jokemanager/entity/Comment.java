package com.duduk.jokemanager.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "joke_id")
    private Joke joke;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "author_name")
    private String authorName; // For anonymous comments

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(String content, Theme theme, User user) {
        this.content = content;
        this.theme = theme;
        this.user = user;
    }

    public Comment(String content, Theme theme, String authorName) {
        this.content = content;
        this.theme = theme;
        this.authorName = authorName;
    }

    public Comment(String content, Joke joke, User user) {
        this.content = content;
        this.joke = joke;
        this.user = user;
    }

    public Comment(String content, Joke joke, String authorName) {
        this.content = content;
        this.joke = joke;
        this.authorName = authorName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }

    public Joke getJoke() { return joke; }
    public void setJoke(Joke joke) { this.joke = joke; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Transient
    public String getDisplayName() {
        return user != null ? user.getUserName() : (authorName != null ? authorName : "匿名用户");
    }
}
