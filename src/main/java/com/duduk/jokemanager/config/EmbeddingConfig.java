package com.duduk.jokemanager.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 嵌入模型配置类
 * 支持本地嵌入和OpenAI嵌入的切换
 */
@Configuration
public class EmbeddingConfig {
    
    @Value("${embedding.provider:local}")
    private String provider;
    
    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;
    
    /**
     * 本地嵌入模型（默认）
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "embedding.provider", havingValue = "local", matchIfMissing = true)
    public EmbeddingModel localEmbeddingModel() {
        return new TransformersEmbeddingModel();
    }
    
    /**
     * OpenAI嵌入模型（可选）
     */
    @Bean
    @ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
    public EmbeddingModel openAiEmbeddingModel() {
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API密钥未配置，请设置spring.ai.openai.api-key");
        }
        
        OpenAiApi openAiApi = new OpenAiApi(openAiApiKey);
        return new OpenAiEmbeddingModel(openAiApi);
    }
}