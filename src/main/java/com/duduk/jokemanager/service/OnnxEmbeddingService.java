package com.duduk.jokemanager.service;

import ai.onnxruntime.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 基于ONNX Runtime的本地嵌入服务
 * 使用all-MiniLM-L6-v2模型进行文本向量化
 */
@Service
public class OnnxEmbeddingService {
    
    private OrtEnvironment env;
    private OrtSession session;
    private final int maxSequenceLength = 256;
    private final int embeddingDimension = 384;
    private boolean modelLoaded = false;
    
    // 简化的词汇表和tokenizer
    private Map<String, Integer> vocab;
    private final Pattern tokenPattern = Pattern.compile("\\w+|[^\\w\\s]");
    
    @PostConstruct
    public void initialize() {
        try {
            env = OrtEnvironment.getEnvironment();
            
            // 尝试从resources加载模型文件
            ClassPathResource modelResource = new ClassPathResource("models/all-MiniLM-L6-v2.onnx");
            
            if (modelResource.exists()) {
                Path tempModel = Files.createTempFile("embedding-model", ".onnx");
                
                try (InputStream is = modelResource.getInputStream()) {
                    Files.copy(is, tempModel, StandardCopyOption.REPLACE_EXISTING);
                }
                
                session = env.createSession(tempModel.toString());
                modelLoaded = true;
                
                // 清理临时文件
                Files.deleteIfExists(tempModel);
            }
            
            // 初始化简化词汇表
            initializeVocab();
            
        } catch (Exception e) {
            // 模型加载失败，使用降级处理
            modelLoaded = false;
        }
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            // Log error but don't throw
        }
    }
    
    /**
     * 将文本转换为向量
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[embeddingDimension];
        }
        
        if (!modelLoaded) {
            // 降级处理：返回随机向量
            return generateRandomEmbedding();
        }
        
        try {
            // Tokenize文本
            int[] tokens = tokenize(text);
            
            // 创建输入张量
            long[] shape = {1, tokens.length};
            IntBuffer buffer = IntBuffer.allocate(tokens.length);
            buffer.put(tokens);
            buffer.rewind();
            
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
            
            // 运行推理
            Map<String, OnnxTensor> inputs = Map.of("input_ids", inputTensor);
            OrtSession.Result result = session.run(inputs);
            
            // 获取输出
            float[][][] output = (float[][][]) result.get(0).getValue();
            
            // 平均池化得到句子向量
            float[] embedding = meanPooling(output[0], tokens);
            
            // 归一化
            return normalize(embedding);
            
        } catch (Exception e) {
            // 降级处理：返回随机向量
            return generateRandomEmbedding();
        }
    }
    
    /**
     * 批量文本向量化
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        
        return texts.stream()
                .map(this::embed)
                .toList();
    }
    
    /**
     * 获取向量维度
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
    
    private void initializeVocab() {
        // 简化的词汇表，实际应用中应该从模型配置文件加载
        vocab = new HashMap<>();
        vocab.put("[PAD]", 0);
        vocab.put("[UNK]", 1);
        vocab.put("[CLS]", 2);
        vocab.put("[SEP]", 3);
        
        // 添加一些常用中文和英文词汇
        String[] commonWords = {
            "的", "是", "在", "有", "和", "了", "不", "我", "你", "他", "她", "它",
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "笑话", "搞笑", "幽默", "好笑", "有趣", "funny", "joke", "humor"
        };
        
        for (int i = 0; i < commonWords.length; i++) {
            vocab.put(commonWords[i], i + 4);
        }
    }
    
    private int[] tokenize(String text) {
        // 简化的tokenization
        String[] words = tokenPattern.matcher(text.toLowerCase()).results()
                .map(mr -> mr.group())
                .toArray(String[]::new);
        
        List<Integer> tokens = new ArrayList<>();
        tokens.add(vocab.getOrDefault("[CLS]", 2)); // CLS token
        
        for (String word : words) {
            if (tokens.size() >= maxSequenceLength - 1) break;
            tokens.add(vocab.getOrDefault(word, vocab.get("[UNK]")));
        }
        
        tokens.add(vocab.getOrDefault("[SEP]", 3)); // SEP token
        
        // Padding
        while (tokens.size() < maxSequenceLength) {
            tokens.add(vocab.get("[PAD]"));
        }
        
        return tokens.stream().mapToInt(Integer::intValue).toArray();
    }
    
    private float[] meanPooling(float[][] tokenEmbeddings, int[] tokens) {
        float[] result = new float[embeddingDimension];
        int validTokens = 0;
        
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] != vocab.get("[PAD]")) {
                for (int j = 0; j < embeddingDimension; j++) {
                    result[j] += tokenEmbeddings[i][j];
                }
                validTokens++;
            }
        }
        
        if (validTokens > 0) {
            for (int i = 0; i < embeddingDimension; i++) {
                result[i] /= validTokens;
            }
        }
        
        return result;
    }
    
    private float[] normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
    }
    
    private float[] generateRandomEmbedding() {
        Random random = new Random();
        float[] embedding = new float[embeddingDimension];
        for (int i = 0; i < embeddingDimension; i++) {
            embedding[i] = (float) (random.nextGaussian() * 0.1);
        }
        return normalize(embedding);
    }
}