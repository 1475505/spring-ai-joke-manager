package com.duduk.jokemanager.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 本地嵌入服务 - 使用Spring AI Transformers
 * 支持多语言文本向量化，无需外部API调用
 */
@Service
public class LocalEmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    
    @Value("${embedding.dimension:384}")
    private int embeddingDimension;
    
    public LocalEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    /**
     * 将文本转换为向量
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[embeddingDimension];
        }
        
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                return response.getResults().get(0).getOutput();
            }
            
            return new float[embeddingDimension];
        } catch (Exception e) {
            // 降级处理：返回零向量
            return new float[embeddingDimension];
        }
    }
    
    /**
     * 批量文本向量化
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            
            return response.getResults().stream()
                    .map(result -> result.getOutput())
                    .toList();
            
        } catch (Exception e) {
            // 降级处理：逐个处理
            return texts.stream()
                    .map(this::embed)
                    .toList();
        }
    }
    
    /**
     * 获取向量维度
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
    
    /**
     * 计算两个向量的余弦相似度
     */
    public float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0f || norm2 == 0.0f) {
            return 0.0f;
        }
        
        return dotProduct / ((float) Math.sqrt(norm1) * (float) Math.sqrt(norm2));
    }
}