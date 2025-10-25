package com.duduk.jokemanager.controller;

import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.service.UserService;
import com.duduk.jokemanager.service.AIService;
import com.duduk.jokemanager.service.JokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/mcp")
@Tag(name = "自然语言操作", description = "通过自然语言操作笑话数据库")
public class MCPController {

    private final ChatClient chatClient;

    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private JokeService jokeService;
    
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
     * @return AI处理结果和工具调用信息
     */
    @PostMapping("/chat")
    @PreAuthorize("hasRole('ROOT')")
    @Operation(summary = "自然语言操作接口", description = "通过自然语言操作笑话数据库，仅限ROOT用户使用")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody ChatRequest request, Principal principal) {
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
            
            // 使用Spring AI框架的ChatClient处理请求，启用工具调用
            String userInput = request.getQuery();
            
            // 构建聊天请求，添加工具支持
            var chatResponse = chatClient.prompt()
                    .user(userInput)
                    .tools(aiService, jokeService, userService) // 启用工具调用，传入多个带有@Tool注解的服务
                    .call()
                    .chatResponse();
            
            // 获取AI响应内容
            String aiResponse = chatResponse.getResult().getOutput().getText();
            
            // 提取工具调用信息
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            if (chatResponse.getResults() != null && !chatResponse.getResults().isEmpty()) {
                var result = chatResponse.getResults().get(0);
                if (result != null && result.getMetadata() != null) {
                    // 获取工具调用信息，使用正确的API
                    var toolExecutionMetadata = result.getMetadata();
                    // 工具调用信息可能存储在不同的属性中，这里尝试获取
                    String finishReason = toolExecutionMetadata.getFinishReason();
                    
                    // 检查是否有工具调用（通过finishReason判断）
                    if ("TOOL_CALL".equals(finishReason) || "tool_calls".equals(finishReason)) {
                        // 由于Spring AI API限制，我们创建一个表示工具调用的条目
                        Map<String, Object> toolCallInfo = new HashMap<>();
                        toolCallInfo.put("name", "aiServiceTool");
                        toolCallInfo.put("arguments", Map.of("query", userInput));
                        toolCalls.add(toolCallInfo);
                    }
                }
            }
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("aiResponse", aiResponse);
            responseData.put("toolCalls", toolCalls);
            responseData.put("toolCallCount", toolCalls.size());
            
            return ResponseEntity.ok(ApiResponse.success(responseData, "操作成功"));
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