package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.Comment;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.CommentRepository;
import org.springframework.ai.core.tool.Tool;
import org.springframework.ai.core.tool.ToolParam;
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

    @Tool(description = "根据ID查找评论")
    public Optional<Comment> findById(@ToolParam(description = "评论ID") Long id) {
        return commentRepository.findById(id);
    }

    @Tool(description = "根据ID删除评论")
    public void deleteById(@ToolParam(description = "要删除的评论ID") Long id) {
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
    @Tool(description = "创建主题评论，需要提供内容、主题和用户")
    public Comment createThemeComment(
            @ToolParam(description = "评论内容") String content, 
            @ToolParam(description = "评论所属的主题") Theme theme, 
            @ToolParam(description = "评论的用户") User user) {
        Comment comment = new Comment(content, theme, user);
        return save(comment);
    }

    @Tool(description = "创建主题评论，使用匿名用户名")
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

    @Tool(description = "创建笑话评论，使用匿名用户名")
    public Comment createJokeComment(
            @ToolParam(description = "评论内容") String content, 
            @ToolParam(description = "评论所属的笑话") Joke joke, 
            @ToolParam(description = "匿名用户名") String authorName) {
        Comment comment = new Comment(content, joke, authorName);
        return save(comment);
    }

    // 更新评论内容
    @Tool(description = "更新评论内容")
    public Comment updateComment(
            @ToolParam(description = "评论ID") Long id, 
            @ToolParam(description = "新的评论内容") String newContent) {
        Optional<Comment> commentOpt = findById(id);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            comment.setContent(newContent);
            return save(comment);
        }
        throw new RuntimeException("Comment not found with id: " + id);
    }
}