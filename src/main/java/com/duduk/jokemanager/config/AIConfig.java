package com.duduk.jokemanager.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    
    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String defaultBaseUrl;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    /**
     * 配置PGVector Store
     * 注意：Spring AI自动配置会创建默认的vectorStore bean，这里不需要重复定义
     */
    // @Bean
    // public VectorStore vectorStore(JdbcTemplate jdbcTemplate) {
    //     return new PgVectorStore(jdbcTemplate, "ai_embeddings");
    // }
    
    /**
     * 创建动态OpenAI客户端
     * 注意：这个方法用于创建默认客户端，实际使用时会根据前端传入的参数动态创建
     */
    @Bean
    public OpenAiChatModel openAiChatModel() {
        if (defaultApiKey.isEmpty()) {
            // 如果没有配置默认API Key，返回一个空的实现
            return null;
        }
        
        OpenAiApi openAiApi = new OpenAiApi(defaultBaseUrl, defaultApiKey);
        
        // 创建ChatOptions并设置model参数
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
            .withModel(defaultModel)
            .build();
        
        return new OpenAiChatModel(openAiApi, chatOptions);
    }
    
    /**
     * 根据前端提供的参数动态创建OpenAI客户端
     */
    public OpenAiChatModel createOpenAiChatModel(String apiKey, String baseUrl, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key不能为空");
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = "https://api.deepseek.com";
        }
        
        if (model == null || model.trim().isEmpty()) {
            model = "deepseek-chat";
        }
        
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        
        // 创建ChatOptions并设置model参数
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
            .withModel(model)
            .build();
        
        return new OpenAiChatModel(openAiApi, chatOptions);
    }
}