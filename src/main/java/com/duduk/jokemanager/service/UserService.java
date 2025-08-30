package com.duduk.jokemanager.service;

import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.UserRepository;
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
    
    public Optional<User> findById(Long id) {
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
    

    
    public User createUser(String username, String password, User.Role role) {
        return createUser(username, null, password, role);
    }
    
    public User createUser(String username, String email, String password, User.Role role) {
        if (existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User(username, passwordEncoder.encode(password), role);
        return save(user);
    }
    
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    public User updateUser(Long userId, User.Role role, String username) {
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