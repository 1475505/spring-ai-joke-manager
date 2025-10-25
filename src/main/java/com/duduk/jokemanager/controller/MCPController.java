package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.service.MCPService;
import com.duduk.jokemanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP自然语言操作", description = "通过自然语言操作笑话数据库")
public class MCPController {

    private final ChatClient chatClient;
    
    @Autowired
    private MCPService mcpService;
    
    @Autowired
    private UserService userService;
    
    // 使用内存聊天记忆
    private final ChatMemory chatMemory = new InMemoryChatMemory();
    
    public MCPController(ChatModel chatModel, MCPService mcpService) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultTools(mcpService)
                .build();
    }
    
    /**
     * 自然语言操作接口
     * @param request 包含用户输入的自然语言
     * @return AI处理结果
     */
    @PostMapping("/chat")
    @Operation(summary = "自然语言操作", description = "通过自然语言操作笑话数据库（仅ROOT用户）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody Map<String, String> request) {
        try {
            // 权限验证：检查是否为ROOT用户
            if (!isRootUser()) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(403, "权限不足，仅ROOT用户可以使用此功能"));
            }
            
            String userMessage = request.get("message");
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "消息内容不能为空"));
            }
            
            // 使用Spring AI ChatClient处理用户消息
            ChatResponse response = chatClient
                    .prompt()
                    .user(userMessage)
                    .call()
                    .chatResponse();
            
            // 构造返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("userMessage", userMessage);
            result.put("aiResponse", response.getResult().getOutput().getText());
            
            return ResponseEntity.ok(ApiResponse.success(result, "处理成功"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "处理自然语言请求时发生错误：" + e.getMessage()));
        }
    }
    
    /**
     * 检查当前用户是否为ROOT用户
     * @return 是否为ROOT用户
     */
    private boolean isRootUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            // 获取认证对象的详细信息
            Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
            
            if (principal instanceof String && "anonymousUser".equals(principal)) {
                return false;
            }
            
            // 直接从JWT用户详情中获取角色信息
            if (details instanceof com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails) {
                com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails userDetails = 
                    (com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails) details;
                // 确保角色不为null并等于"ROOT"
                return userDetails.getRole() != null && "ROOT".equals(userDetails.getRole());
            }
            
            // 如果JWT用户详情不可用，则通过用户ID查询数据库
            Long userId = null;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // JWT用户详情
                if (details instanceof com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails) {
                    userId = ((com.duduk.jokemanager.config.JwtAuthenticationFilter.JwtUserDetails) details).getUserId();
                }
            }
            
            if (userId == null) {
                return false;
            }
            
            // 查询用户信息
            Optional<User> userOpt = userService.findById(userId);
            return userOpt.isPresent() && userOpt.get().isRoot();
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常信息以便调试
            return false;
        }
    }
}