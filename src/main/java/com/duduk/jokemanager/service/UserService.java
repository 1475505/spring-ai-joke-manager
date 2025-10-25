package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.UserRepository;
import org.springframework.ai.core.tool.Tool;
import org.springframework.ai.core.tool.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUserName(username);
    }
    
    @Tool(description = "根据ID查找用户信息")
    public Optional<User> findById(@ToolParam(description = "用户ID") Long id) {
        return userRepository.findById(id);
    }
    
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUserName(username);
    }
    

    
    @Tool(description = "创建新用户，需要提供用户名、密码和角色")
    public User createUser(
            @ToolParam(description = "用户名") String username, 
            @ToolParam(description = "密码") String password, 
            @ToolParam(description = "用户角色，可选值：USER, ADMIN, ROOT") User.Role role) {
        return createUser(username, null, password, role);
    }
    
    @Tool(description = "创建新用户，可以提供邮箱地址")
    public User createUser(
            @ToolParam(description = "用户名") String username, 
            @ToolParam(description = "邮箱地址，可选参数") String email, 
            @ToolParam(description = "密码") String password, 
            @ToolParam(description = "用户角色，可选值：USER, ADMIN, ROOT") User.Role role) {
        if (existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User(username, passwordEncoder.encode(password), role);
        return save(user);
    }
    
    @Tool(description = "根据ID删除用户")
    public void deleteUser(@ToolParam(description = "要删除的用户ID") Long userId) {
        userRepository.deleteById(userId);
    }
    
    @Tool(description = "更新用户信息，可以修改角色和用户名")
    public User updateUser(
            @ToolParam(description = "要更新的用户ID") Long userId, 
            @ToolParam(description = "新的用户角色，可选值：USER, ADMIN, ROOT") User.Role role, 
            @ToolParam(description = "新的用户名，可选参数") String username) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        User user = userOpt.get();
        if (role != null) {
            user.setRole(role);
        }
        if (username != null && !username.equals(user.getUserName())) {
            if (existsByUsername(username)) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUserName(username);
        }
        
        return save(user);
    }
    
    public void resetPassword(Long userId, String newPassword) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        
        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        save(user);
    }
}