package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/mcp")
@Tag(name = "自然语言操作", description = "通过自然语言操作笑话数据库")
public class MCPController {

    private final ChatClient chatClient;

    
    @Autowired
    private UserService userService;
    
    // 使用内存聊天记忆
    private final ChatMemory chatMemory = null; // 暂时禁用聊天记忆功能
    
    public MCPController(ChatModel chatModel) {
        if (chatModel != null) {
            this.chatClient = ChatClient.builder(chatModel)
                    .build();
        } else {
            this.chatClient = null;
        }
    }
    
    /**
     * 自然语言操作接口
     * @param request 包含用户输入的自然语言
     * @return AI处理结果
     */
    @PostMapping("/chat")
    @PreAuthorize("hasRole('ROOT')")
    @Operation(summary = "自然语言操作接口", description = "通过自然语言操作笑话数据库，仅限ROOT用户使用")
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody ChatRequest request, Principal principal) {
        try {
            // 获取当前用户ID
            Long userId = null;
            Object details = ((org.springframework.security.core.Authentication) principal).getDetails();
            // 正确引用JwtUserDetails类
            if (details instanceof com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails) {
                userId = ((com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails) details).getUserId();
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "无法获取用户信息"));
            }
            
            // 验证用户是否为ROOT
            Optional<User> userOpt = userService.findById(userId);
            if (!userOpt.isPresent() || !userOpt.get().isRoot()) {
                return ResponseEntity.status(403).body(ApiResponse.error(403, "权限不足，仅限ROOT用户使用"));
            }
            
            // 检查ChatClient是否可用
            if (chatClient == null) {
                return ResponseEntity.status(500).body(ApiResponse.error(500, "AI服务未配置或不可用，请检查API密钥配置"));
            }
            
            // 使用Spring AI框架的ChatClient处理请求
            String userInput = request.getQuery();
            String aiResponse = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .content();
            
            return ResponseEntity.ok(ApiResponse.success(aiResponse, "操作成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error(500, "处理失败：" + e.getMessage()));
        }
    }
    
    /**
     * 请求体类
     */
    public static class ChatRequest {
        private String query;
        
        public ChatRequest() {}
        
        public ChatRequest(String query) {
            this.query = query;
        }
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
    }
    

}