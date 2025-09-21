package com.duduk.jokemanager.config;

import com.duduk.jokemanager.service.LocalEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 嵌入模型配置类
 * 使用本地ONNX嵌入模型
 */
@Configuration
public class EmbeddingConfig {
    
    @Autowired
    private LocalEmbeddingModel localEmbeddingModel;
    
    /**
     * 本地嵌入模型
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return localEmbeddingModel;
    }
}