package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.JokeRepository;
import com.duduk.jokemanager.repository.ThemeRepository;
import com.duduk.jokemanager.repository.UserRepository;
import com.duduk.jokemanager.service.JokeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MCPService {
    
    @Autowired
    private JokeService jokeService;
    
    @Autowired
    private JokeRepository jokeRepository;
    
    @Autowired
    private ThemeRepository themeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 查询笑话列表
     * @param count 查询数量
     * @param keyword 关键词
     * @param themeName 主题名称
     * @return 笑话列表
     */
    /**
     * 查询笑话列表 - 工具方法: query_jokes
     */
    public String queryJokes(
            String count,
            String keyword,
            String themeName) {
        
        try {
            // 默认查询10条
            int countValue = 10;
            if (count != null && !count.trim().isEmpty()) {
                try {
                    countValue = Integer.parseInt(count.trim());
                } catch (NumberFormatException e) {
                    // 使用默认值
                }
            }
            
            // 限制最大查询数量
            if (countValue > 50) {
                countValue = 50;
            }
            
            Pageable pageable = PageRequest.of(0, countValue, Sort.by("createdAt").descending());
            
            // 根据主题名称查找主题ID
            Long themeId = null;
            if (themeName != null && !themeName.trim().isEmpty()) {
                Optional<Theme> themeOpt = themeRepository.findByName(themeName.trim());
                if (themeOpt.isPresent()) {
                    themeId = themeOpt.get().getId();
                }
            }
            
            // 调用笑话服务获取笑话列表
            Page<Joke> jokes = jokeService.getJokes(
                themeId, 
                Joke.Status.APPROVED, 
                null, 
                null, 
                keyword, 
                null, 
                pageable
            );
            
            if (jokes.isEmpty()) {
                return "未找到符合条件的笑话";
            }
            
            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append("查询到 ").append(jokes.getContent().size()).append(" 条笑话：\n");
            
            for (int i = 0; i < jokes.getContent().size(); i++) {
                Joke joke = jokes.getContent().get(i);
                result.append(i + 1).append(". ")
                      .append(joke.getTitle() != null ? joke.getTitle() : "无标题")
                      .append("\n   内容：").append(joke.getContent().substring(0, Math.min(50, joke.getContent().length())))
                      .append(joke.getContent().length() > 50 ? "..." : "")
                      .append("\n   评分：").append(joke.getFinalScore())
                      .append("\n   主题：").append(joke.getTheme().getName())
                      .append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "查询笑话时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 创建新笑话
     * @param title 笑话标题
     * @param content 笑话内容
     * @param themeName 主题名称
     * @return 创建结果
     */
    /**
     * 创建新笑话 - 工具方法: create_joke
     */
    public String createJoke(
            String title,
            String content,
            String themeName) {
        
        try {
            // 验证参数
            if (content == null || content.trim().isEmpty()) {
                return "笑话内容不能为空";
            }
            
            if (themeName == null || themeName.trim().isEmpty()) {
                return "主题名称不能为空";
            }
            
            // 查找主题
            Optional<Theme> themeOpt = themeRepository.findByName(themeName.trim());
            if (themeOpt.isEmpty()) {
                return "未找到主题: " + themeName;
            }
            
            Theme theme = themeOpt.get();
            
            // 创建ROOT用户（用于MCP操作）
            User rootUser = getOrCreateRootUser();
            
            // 创建笑话
            Joke joke = new Joke();
            joke.setTitle(title);
            joke.setContent(content);
            joke.setTheme(theme);
            joke.setCreatedBy(rootUser);
            joke.setStatus(Joke.Status.APPROVED); // MCP创建的笑话默认为已审核状态
            
            Joke savedJoke = jokeRepository.save(joke);
            
            return "成功创建笑话，ID: " + savedJoke.getId() + 
                   ", 标题: " + (savedJoke.getTitle() != null ? savedJoke.getTitle() : "无标题");
        } catch (Exception e) {
            return "创建笑话时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 删除笑话
     * @param jokeId 笑话ID
     * @return 删除结果
     */
    /**
     * 删除笑话 - 工具方法: delete_joke
     */
    public String deleteJoke(
            String jokeId) {
        
        try {
            if (jokeId == null || jokeId.trim().isEmpty()) {
                return "笑话ID不能为空";
            }
            
            Long jokeIdValue = null;
            try {
                jokeIdValue = Long.parseLong(jokeId.trim());
            } catch (NumberFormatException e) {
                return "笑话ID格式不正确";
            }
            
            Optional<Joke> jokeOpt = jokeRepository.findById(jokeIdValue);
            if (jokeOpt.isEmpty()) {
                return "未找到ID为 " + jokeIdValue + " 的笑话";
            }
            
            Joke joke = jokeOpt.get();
            String jokeTitle = joke.getTitle() != null ? joke.getTitle() : "无标题";
            
            jokeRepository.deleteById(jokeIdValue);
            
            return "成功删除笑话，ID: " + jokeIdValue + ", 标题: " + jokeTitle;
        } catch (Exception e) {
            return "删除笑话时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 获取或创建ROOT用户
     * @return ROOT用户
     */
    private User getOrCreateRootUser() {
        Optional<User> rootUserOpt = userRepository.findByUserName("ROOT");
        if (rootUserOpt.isPresent()) {
            return rootUserOpt.get();
        }
        
        // 创建ROOT用户
        User rootUser = new User();
        rootUser.setUserName("ROOT");
        rootUser.setPasswordHash(passwordEncoder.encode("root")); // 使用正确的setter方法
        rootUser.setRole(User.Role.ROOT);
        return userRepository.save(rootUser);
    }
    
    /**
     * 获取随机笑话
     * @param count 数量
     * @param themeName 主题名称
     * @return 随机笑话列表
     */
    /**
     * 获取随机笑话 - 工具方法: get_random_jokes
     */
    public String getRandomJokes(
            String count,
            String themeName) {
        
        try {
            // 默认获取1条，最多10条
            int countValue = 1;
            if (count != null && !count.trim().isEmpty()) {
                try {
                    countValue = Integer.parseInt(count.trim());
                } catch (NumberFormatException e) {
                    // 使用默认值
                }
            }
            
            if (countValue <= 0) {
                countValue = 1;
            }
            if (countValue > 10) {
                countValue = 10;
            }
            
            // 根据主题名称查找主题ID
            Long themeId = null;
            if (themeName != null && !themeName.trim().isEmpty()) {
                Optional<Theme> themeOpt = themeRepository.findByName(themeName.trim());
                if (themeOpt.isPresent()) {
                    themeId = themeOpt.get().getId();
                }
            }
            
            // 获取随机笑话
            List<Joke> randomJokes = jokeService.getRandomJokes(themeId, new BigDecimal("0.0"), countValue);
            
            if (randomJokes.isEmpty()) {
                return "未找到符合条件的随机笑话";
            }
            
            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append("随机获取到 ").append(randomJokes.size()).append(" 条笑话：\n");
            
            for (int i = 0; i < randomJokes.size(); i++) {
                Joke joke = randomJokes.get(i);
                result.append(i + 1).append(". ")
                      .append(joke.getTitle() != null ? joke.getTitle() : "无标题")
                      .append("\n   内容：").append(joke.getContent().substring(0, Math.min(50, joke.getContent().length())))
                      .append(joke.getContent().length() > 50 ? "..." : "")
                      .append("\n   评分：").append(joke.getFinalScore())
                      .append("\n   主题：").append(joke.getTheme().getName())
                      .append("\n\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "获取随机笑话时发生错误: " + e.getMessage();
        }
    }
}