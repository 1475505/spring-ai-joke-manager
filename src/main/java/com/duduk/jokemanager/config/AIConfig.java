package com.duduk.jokemanager.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AIConfig {
    
    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String defaultBaseUrl;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String defaultModel;
    
    /**
     * 配置PgVectorStore，使用字符串ID类型而不是UUID
     * 因为PgVectorStore默认使用UUID作为主键，而我们的笑话ID是Long类型
     * 这会导致向量数据库操作时出现UUID格式错误
     * 
     * 解决方案：配置PgVectorStore使用字符串ID类型
     */
    
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
            .model(defaultModel)
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
            .model(model)
            .build();
        
        return new OpenAiChatModel(openAiApi, chatOptions);
    }
    
    /**
     * 配置PgVectorStore - 使用PostgreSQL的pgvector扩展进行向量存储
     * 支持RAG功能，将笑话内容向量化存储以便进行相似性搜索
     * 配置为384维向量，匹配本地ONNX嵌入模型
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)  // 自动初始化数据库schema
                .removeExistingVectorStoreTable(true)  // 删除现有表，重新创建
                .build();
    }
}