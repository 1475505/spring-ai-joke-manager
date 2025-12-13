package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.CommentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    // 基础CRUD操作
    public Comment save(@ToolParam(description = "评论对象") Comment comment) {
        return commentRepository.save(comment);
    }

    public Optional<Comment> findById(@ToolParam(description = "评论ID") Long id) {
        return commentRepository.findById(id);
    }

    public void deleteById(@ToolParam(description = "要删除的评论ID") Long id) {
        commentRepository.deleteById(id);
    }

    public Page<Comment> findAll(@ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findAll(pageable);
    }

    // 主题评论相关方法
    public Page<Comment> findByTheme(
            @ToolParam(description = "主题对象") Theme theme, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findByTheme(theme, pageable);
    }

    public List<Comment> findByThemeOrderByCreatedAtDesc(@ToolParam(description = "主题对象") Theme theme) {
        return commentRepository.findByThemeOrderByCreatedAtDesc(theme);
    }

    public Page<Comment> findByThemeAndKeyword(
            @ToolParam(description = "主题对象") Theme theme, 
            @ToolParam(description = "搜索关键词") String keyword, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findByThemeAndKeyword(theme, keyword, pageable);
    }

    public Long countByTheme(@ToolParam(description = "主题对象") Theme theme) {
        return commentRepository.countByTheme(theme);
    }

    // 笑话评论相关方法
    public Page<Comment> findByJoke(
            @ToolParam(description = "笑话对象") Joke joke, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findByJoke(joke, pageable);
    }

    public List<Comment> findByJokeOrderByCreatedAtDesc(@ToolParam(description = "笑话对象") Joke joke) {
        return commentRepository.findByJokeOrderByCreatedAtDesc(joke);
    }

    public Page<Comment> findByJokeAndKeyword(
            @ToolParam(description = "笑话对象") Joke joke, 
            @ToolParam(description = "搜索关键词") String keyword, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findByJokeAndKeyword(joke, keyword, pageable);
    }

    public Long countByJoke(@ToolParam(description = "笑话对象") Joke joke) {
        return commentRepository.countByJoke(joke);
    }

    // 用户评论相关方法
    public Page<Comment> findByUser(
            @ToolParam(description = "用户对象") User user, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findByUser(user, pageable);
    }

    public List<Comment> findByUserOrderByCreatedAtDesc(@ToolParam(description = "用户对象") User user) {
        return commentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<Comment> findByAuthor(
            @ToolParam(description = "用户对象") User user, 
            @ToolParam(description = "搜索关键词") String keyword, 
            @ToolParam(description = "分页信息") Pageable pageable) {
        return commentRepository.findByAuthor(user, keyword, pageable);
    }

    public Long countByUser(@ToolParam(description = "用户对象") User user) {
        return commentRepository.countByUser(user);
    }

    // 创建主题评论
    @Tool(description = "创建主题评论，需要提供内容、主题和用户")
    public Comment createThemeComment(
            @ToolParam(description = "评论内容") String content, 
            @ToolParam(description = "评论所属的主题") Theme theme, 
            @ToolParam(description = "评论的用户") User user) {
        Comment comment = new Comment(content, theme, user);
        return save(comment);
    }

    public Comment createThemeComment(
            @ToolParam(description = "评论内容") String content, 
            @ToolParam(description = "评论所属的主题") Theme theme, 
            @ToolParam(description = "匿名用户名") String authorName) {
        Comment comment = new Comment(content, theme, authorName);
        return save(comment);
    }

    // 创建笑话评论
    @Tool(description = "创建笑话评论，需要提供内容、笑话和用户")
    public Comment createJokeComment(
            @ToolParam(description = "评论内容") String content, 
            @ToolParam(description = "评论所属的笑话") Joke joke, 
            @ToolParam(description = "评论的用户") User user) {
        Comment comment = new Comment(content, joke, user);
        return save(comment);
    }

    public Comment createJokeComment(
            @ToolParam(description = "评论内容") String content, 
            @ToolParam(description = "评论所属的笑话") Joke joke, 
            @ToolParam(description = "匿名用户名") String authorName) {
        Comment comment = new Comment(content, joke, authorName);
        return save(comment);
    }

    // 更新评论内容
    public Comment updateComment(
            @ToolParam(description = "评论ID") Long id, 
            @ToolParam(description = "新的评论内容") String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        comment.setContent(content);
        return commentRepository.save(comment);
    }
}