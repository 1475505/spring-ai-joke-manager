package com.duduk.jokemanager.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.service.LocalEmbeddingService;

@Repository
public class VectorRepository {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private LocalEmbeddingService embeddingService;
    
    /**
     * 添加笑话到向量数据库
     */
    public void addJoke(Joke joke) {
        if (joke.getTheme() == null || joke.getContent() == null) {
            return;
        }
        
        // 使用本地嵌入服务生成向量
        float[] embedding = embeddingService.embed(joke.getContent());
        
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
        
        // Spring AI M6版本不再支持直接设置embedding，由VectorStore自动处理
        
        vectorStore.add(List.of(document));
    }
    
    /**
     * 批量添加笑话到向量数据库
     */
    public void addJokes(List<Joke> jokes) {
        List<String> contents = jokes.stream()
            .filter(joke -> joke.getTheme() != null && joke.getContent() != null)
            .map(Joke::getContent)
            .collect(Collectors.toList());
        
        // 批量生成向量
        List<float[]> embeddings = embeddingService.embedBatch(contents);
        
        List<Document> documents = jokes.stream()
            .filter(joke -> joke.getTheme() != null && joke.getContent() != null)
            .map(joke -> {
                Document doc = new Document(
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
                // Spring AI M6版本不再支持直接设置embedding，由VectorStore自动处理
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
        SearchRequest searchRequest = SearchRequest.builder()
            .query(content)
            .topK(topK)
            .similarityThreshold(0.7)
            .build();
        
        return vectorStore.similaritySearch(searchRequest);
    }
    
    /**
     * 根据主题搜索相似笑话
     */
    public List<Document> searchSimilarJokesByTheme(String content, Long themeId, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(content)
            .topK(topK)
            .similarityThreshold(0.7)
            .filterExpression("theme_id == '" + themeId + "'")
            .build();
        
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
     * 更新笑话
     */
    public void updateJoke(Joke joke) {
        // 先删除旧的
        deleteJoke(joke.getId());
        // 再添加新的
        addJoke(joke);
    }
    

}