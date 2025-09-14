package com.duduk.jokemanager.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.duduk.jokemanager.entity.Joke;

@Repository
public class VectorRepository {
    
    @Autowired
    private VectorStore vectorStore;
    
    /**
     * 添加笑话到向量数据库
     */
    public void addJoke(Joke joke) {
        if (joke.getTheme() == null || joke.getContent() == null) {
            return;
        }
        
        Document document = new Document(
            joke.getId().toString(), // 使用joke ID作为文档ID
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
            .map(joke -> new Document(
                joke.getId().toString(), // 使用joke ID作为文档ID
                joke.getContent(),
                Map.of(
                    "id", joke.getId().toString(),
                    "title", joke.getTitle() != null ? joke.getTitle() : "",
                    "theme_id", joke.getTheme().getId().toString(),
                    "theme_name", joke.getTheme().getName() != null ? joke.getTheme().getName() : "",
                    "score", joke.getFinalScore() != null ? joke.getFinalScore().toString() : "7.0",
                    "is_ai_generated", joke.getIsAiGenerate() != null ? joke.getIsAiGenerate().toString() : "false"
                )
            ))
            .collect(Collectors.toList());
        
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }
    
    /**
     * 根据内容相似性搜索笑话
     */
    public List<Document> searchSimilarJokes(String content, int topK) {
        SearchRequest searchRequest = SearchRequest.query(content)
            .withTopK(topK)
            .withSimilarityThreshold(0.7);
        
        return vectorStore.similaritySearch(searchRequest);
    }
    
    /**
     * 根据主题搜索相似笑话
     */
    public List<Document> searchSimilarJokesByTheme(String content, Long themeId, int topK) {
        SearchRequest searchRequest = SearchRequest.query(content)
            .withTopK(topK)
            .withSimilarityThreshold(0.7)
            .withFilterExpression("theme_id == '" + themeId + "'");
        
        return vectorStore.similaritySearch(searchRequest);
    }
    
    /**
     * 删除笑话从向量数据库
     */
    public void deleteJoke(Long jokeId) {
        vectorStore.delete(List.of(jokeId.toString()));
    }
    
    /**
     * 批量删除笑话从向量数据库
     */
    public void deleteJokes(List<Long> jokeIds) {
        List<String> ids = jokeIds.stream()
            .map(Object::toString)
            .collect(Collectors.toList());
        
        vectorStore.delete(ids);
    }
    
    /**
     * 更新笑话在向量数据库中的信息
     */
    public void updateJoke(Joke joke) {
        // 先删除旧的
        deleteJoke(joke.getId());
        // 再添加新的
        addJoke(joke);
    }
    
    /**
     * 清空指定主题的所有向量数据
     */
    public void clearThemeVectors(Long themeId) {
        // 由于PGVector Store可能不支持按条件批量删除，这里使用搜索然后删除的方式
        SearchRequest searchRequest = SearchRequest.query("*")
            .withTopK(1000)
            .withFilterExpression("theme_id == '" + themeId + "'");
        
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        List<String> ids = documents.stream()
            .map(doc -> doc.getMetadata().get("id").toString())
            .collect(Collectors.toList());
        
        if (!ids.isEmpty()) {
            vectorStore.delete(ids);
        }
    }
    
    /**
     * 检查笑话是否存在重复内容
     */
    public boolean isDuplicateContent(String content, Long themeId, double threshold) {
        try {
            List<Document> similarDocs = searchSimilarJokesByTheme(content, themeId, 5);
            
            // 如果找到相似度很高的文档，认为是重复内容
            return similarDocs.stream()
                .anyMatch(doc -> {
                    // 这里需要根据实际的相似度计算方式来判断
                    // 暂时使用简单的文本包含判断
                    String docContent = doc.getContent();
                    return docContent.contains(content) || content.contains(docContent);
                });
        } catch (Exception e) {
             // 如果向量数据库查询失败，返回false（不重复）
             return false;
         }
     }
    
    /**
     * 获取主题下的笑话统计信息
     */
    public Map<String, Object> getThemeStatistics(Long themeId) {
        try {
            SearchRequest searchRequest = SearchRequest.query("*")
                .withTopK(1000)
                .withFilterExpression("theme_id == '" + themeId + "'");
            
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            long totalCount = documents.size();
            long aiGeneratedCount = documents.stream()
                .mapToLong(doc -> "true".equals(doc.getMetadata().get("is_ai_generated")) ? 1 : 0)
                .sum();
            
            return Map.of(
                "totalJokes", totalCount,
                "aiGeneratedJokes", aiGeneratedCount,
                "humanCreatedJokes", totalCount - aiGeneratedCount
            );
        } catch (Exception e) {
            // 如果向量数据库查询失败，返回默认值
            return Map.of(
                "totalJokes", 0L,
                "aiGeneratedJokes", 0L,
                "humanCreatedJokes", 0L
            );
        }
    }
}