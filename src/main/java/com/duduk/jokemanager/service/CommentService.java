package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    // 基础CRUD操作
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    public void deleteById(Long id) {
        commentRepository.deleteById(id);
    }

    public Page<Comment> findAll(Pageable pageable) {
        return commentRepository.findAll(pageable);
    }

    // 主题评论相关方法
    public Page<Comment> findByTheme(Theme theme, Pageable pageable) {
        return commentRepository.findByTheme(theme, pageable);
    }

    public List<Comment> findByThemeOrderByCreatedAtDesc(Theme theme) {
        return commentRepository.findByThemeOrderByCreatedAtDesc(theme);
    }

    public Page<Comment> findByThemeAndKeyword(Theme theme, String keyword, Pageable pageable) {
        return commentRepository.findByThemeAndKeyword(theme, keyword, pageable);
    }

    public Long countByTheme(Theme theme) {
        return commentRepository.countByTheme(theme);
    }

    // 笑话评论相关方法
    public Page<Comment> findByJoke(Joke joke, Pageable pageable) {
        return commentRepository.findByJoke(joke, pageable);
    }

    public List<Comment> findByJokeOrderByCreatedAtDesc(Joke joke) {
        return commentRepository.findByJokeOrderByCreatedAtDesc(joke);
    }

    public Page<Comment> findByJokeAndKeyword(Joke joke, String keyword, Pageable pageable) {
        return commentRepository.findByJokeAndKeyword(joke, keyword, pageable);
    }

    public Long countByJoke(Joke joke) {
        return commentRepository.countByJoke(joke);
    }

    // 用户评论相关方法
    public Page<Comment> findByUser(User user, Pageable pageable) {
        return commentRepository.findByUser(user, pageable);
    }

    public List<Comment> findByUserOrderByCreatedAtDesc(User user) {
        return commentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<Comment> findByAuthor(User user, String keyword, Pageable pageable) {
        return commentRepository.findByAuthor(user, keyword, pageable);
    }

    public Long countByUser(User user) {
        return commentRepository.countByUser(user);
    }

    // 创建主题评论
    public Comment createThemeComment(String content, Theme theme, User user) {
        Comment comment = new Comment(content, theme, user);
        return save(comment);
    }

    public Comment createThemeComment(String content, Theme theme, String authorName) {
        Comment comment = new Comment(content, theme, authorName);
        return save(comment);
    }

    // 创建笑话评论
    public Comment createJokeComment(String content, Joke joke, User user) {
        Comment comment = new Comment(content, joke, user);
        return save(comment);
    }

    public Comment createJokeComment(String content, Joke joke, String authorName) {
        Comment comment = new Comment(content, joke, authorName);
        return save(comment);
    }

    // 更新评论内容
    public Comment updateComment(Long id, String newContent) {
        Optional<Comment> commentOpt = findById(id);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            comment.setContent(newContent);
            return save(comment);
        }
        throw new RuntimeException("Comment not found with id: " + id);
    }
}