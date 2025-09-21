package com.duduk.jokemanager.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 本地嵌入模型实现
 * 基于ONNX Runtime的Spring AI EmbeddingModel接口实现
 */
@Component
public class LocalEmbeddingModel implements EmbeddingModel {
    
    @Autowired
    private OnnxEmbeddingService onnxEmbeddingService;
    
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        
        List<Embedding> results = IntStream.range(0, texts.size())
                .mapToObj(i -> {
                    float[] embedding = onnxEmbeddingService.embed(texts.get(i));
                    return new Embedding(embedding, i);
                })
                .toList();
        
        return new EmbeddingResponse(results);
    }
    
    @Override
    public float[] embed(Document document) {
        return onnxEmbeddingService.embed(document.getText());
    }
    
    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        List<Embedding> results = IntStream.range(0, texts.size())
                .mapToObj(i -> {
                    float[] embedding = onnxEmbeddingService.embed(texts.get(i));
                    return new Embedding(embedding, i);
                })
                .toList();
        
        return new EmbeddingResponse(results);
    }
    
    /**
     * 获取嵌入维度
     */
    public int dimensions() {
        return onnxEmbeddingService.getEmbeddingDimension();
    }
}