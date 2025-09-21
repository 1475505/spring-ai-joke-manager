package com.duduk.jokemanager.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.duduk.jokemanager.entity.Joke;

@Repository
public class VectorRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorRepository.class);
    
    @Autowired
    private VectorStore vectorStore;
    
    /**
     * 添加笑话到向量数据库
     */
    public void addJoke(Joke joke) {
        if (joke.getTheme() == null || joke.getContent() == null) {
            return;
        }
        
        // 生成UUID格式的文档ID
        String documentId = String.format("%08d-0000-0000-0000-000000000000", joke.getId());
        
        Document document = new Document(
            documentId,
            joke.getContent(),
            Map.of(
                "id", joke.getId().toString(),
                "title", joke.getTitle() != null ? joke.getTitle() : "",
                "theme_id", joke.getTheme().getId().toString(),
                "theme_name", joke.getTheme().getName() != null ? joke.getTheme().getName() : "",
                "score", joke.getFinalScore() != null ? joke.getFinalScore().toString() : "7.0",
                "is_ai_generated", joke.getIsAiGenerate() != null ? joke.getIsAiGenerate().toString() : "false"
            )
        );
        
        vectorStore.add(List.of(document));
    }
    
    /**
     * 批量添加笑话到向量数据库
     */
    public void addJokes(List<Joke> jokes) {
        List<Document> documents = jokes.stream()
            .filter(joke -> joke.getTheme() != null && joke.getContent() != null)
            .map(joke -> {
                // 生成UUID格式的文档ID
                String documentId = String.format("%08d-0000-0000-0000-000000000000", joke.getId());
                
                Document doc = new Document(
                    documentId,
                    joke.getContent(),
                    Map.of(
                        "id", joke.getId().toString(),
                        "title", joke.getTitle() != null ? joke.getTitle() : "",
                        "theme_id", joke.getTheme().getId().toString(),
                        "theme_name", joke.getTheme().getName() != null ? joke.getTheme().getName() : "",
                        "score", joke.getFinalScore() != null ? joke.getFinalScore().toString() : "7.0",
                        "is_ai_generated", joke.getIsAiGenerate() != null ? joke.getIsAiGenerate().toString() : "false"
                    )
                );
                return doc;
            })
            .collect(Collectors.toList());
        
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }
    
    /**
     * 根据内容相似性搜索笑话
     */
    public List<Document> searchSimilarJokes(String content, int topK) {
        logger.info("开始向量搜索 - 查询内容: '{}', topK: {}", content, topK);
        
        SearchRequest searchRequest = SearchRequest.builder()
            .query(content)
            .topK(topK)
            .similarityThreshold(0.01)  // 设置合适的相似度阈值
            .build();
        
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        logger.info("向量搜索完成 - 查询内容: '{}', 召回结果数量: {}", content, results.size());
        
        // 输出召回结果的相似度分数（降序排列）
        if (!results.isEmpty()) {
            logger.info("RAG召回结果相似度分数（降序）:");
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                double score = doc.getScore() != null ? doc.getScore() : 0.0;
                String jokeId = (String) doc.getMetadata().get("id");
                String contentPreview = doc.getText().length() > 50 ? doc.getText().substring(0, 50) + "..." : doc.getText();
                logger.info("  第{}名: ID={}, 相似度分数={}, 内容='{}'", 
                    i + 1, jokeId, String.format("%.4f", score), contentPreview);
            }
        }
        
        return results;
    }
    
    /**
     * 根据主题搜索相似笑话
     */
    public List<Document> searchSimilarJokesByTheme(String content, Long themeId, int topK) {
        logger.info("开始主题向量搜索 - 查询内容: '{}', 主题ID: {}, topK: {}", content, themeId, topK);
        
        SearchRequest searchRequest = SearchRequest.builder()
            .query(content)
            .topK(topK)
            .similarityThreshold(0.01)  // 基于实际测试结果设置阈值，召回相似度>0.01的结果
            .filterExpression("theme_id == '" + themeId + "'")
            .build();
        
        logger.info("向量搜索请求详情 - 查询: '{}', topK: {}, 过滤条件: theme_id == '{}'", content, topK, themeId);
        
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        logger.info("主题向量搜索完成 - 查询内容: '{}', 主题ID: {}, 召回结果数量: {}", content, themeId, results.size());
        
        // 输出召回结果的相似度分数（降序排列）
        logger.info("主题RAG召回结果相似度分数（降序）:");
        if (!results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                double score = doc.getScore() != null ? doc.getScore() : 0.0;
                String jokeId = (String) doc.getMetadata().get("id");
                String themeIdStr = (String) doc.getMetadata().get("theme_id");
                String contentPreview = doc.getText().length() > 50 ? doc.getText().substring(0, 50) + "..." : doc.getText();
                logger.info("  第{}名: ID={}, 主题ID={}, 相似度分数={}, 内容='{}'", 
                    i + 1, jokeId, themeIdStr, String.format("%.4f", score), contentPreview);
            }
        } else {
            logger.info("  无召回结果");
        }
        
        return results;
    }
    
    /**
     * 删除笑话从向量数据库
     */
    public void deleteJoke(Long jokeId) {
        String documentId = String.format("%08d-0000-0000-0000-000000000000", jokeId);
        vectorStore.delete(List.of(documentId));
    }
    
    /**
     * 批量删除笑话从向量数据库
     */
    public void deleteJokes(List<Long> jokeIds) {
        List<String> ids = jokeIds.stream()
            .map(id -> String.format("%08d-0000-0000-0000-000000000000", id))
            .collect(Collectors.toList());
        
        vectorStore.delete(ids);
    }
    
    /**
     * 更新笑话
     */
    public void updateJoke(Joke joke) {
        // 先删除旧的
        deleteJoke(joke.getId());
        // 再添加新的
        addJoke(joke);
    }
    
    /**
     * 检查内容是否重复
     */
    public boolean isDuplicateContent(String content, Long themeId, double threshold) {
        List<Document> similarJokes = searchSimilarJokesByTheme(content, themeId, 5);
        
        for (Document doc : similarJokes) {
            // 这里简化处理，实际应该计算相似度分数
            if (doc.getText().equals(content)) {
                return true;
            }
        }
        
        return false;
    }
}