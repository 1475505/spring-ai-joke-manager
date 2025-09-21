package com.duduk.jokemanager.service;

import com.duduk.jokemanager.config.AIConfig;
import com.duduk.jokemanager.entity.Joke;
import com.duduk.jokemanager.entity.Theme;
import com.duduk.jokemanager.entity.User;
import com.duduk.jokemanager.repository.JokeRepository;
import com.duduk.jokemanager.repository.ThemeRepository;
import com.duduk.jokemanager.repository.UserRepository;
import com.duduk.jokemanager.repository.VectorRepository;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    @Autowired
    private JokeRepository jokeRepository;
    
    @Autowired
    private ThemeRepository themeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JokeService jokeService;
    
    @Autowired
    private VectorRepository vectorRepository;
    
    @Autowired
    private AIConfig aiConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * AI智能评分
     */
    public Map<String, Object> aiScore(Map<String, Object> request) {
        try {
            // 处理jokeId的类型转换，支持Integer和String
            Object jokeIdObj = request.get("jokeId");
            Long jokeId;
            if (jokeIdObj instanceof Integer) {
                jokeId = ((Integer) jokeIdObj).longValue();
            } else if (jokeIdObj instanceof String) {
                jokeId = Long.valueOf((String) jokeIdObj);
            } else {
                throw new RuntimeException("无效的jokeId类型");
            }
            
            String apiKey = (String) request.get("apiKey");
            String modelName = (String) request.getOrDefault("modelName", "deepseek-chat");
            String baseUrl = (String) request.getOrDefault("baseUrl", "https://api.deepseek.com");
            
            Optional<Joke> jokeOpt = jokeRepository.findById(jokeId);
            if (jokeOpt.isEmpty()) {
                throw new RuntimeException("笑话不存在");
            }
            
            Joke joke = jokeOpt.get();
            Theme theme = joke.getTheme();
            
            // 构建评分提示词
            String prompt = String.format(
                "请对主题为【%s】的笑话进行评分。主题说明：%s；笑话内容：%s；评分标准：10分-非常优秀的笑话，有很强的创意和笑点；8分-优秀的笑话，有明显笑点；6分-一般的笑话，无法让用户笑起来；4分-偏离主题，不知所云，或没有笑点的笑话；2分-和主题有一部分相关性，但不算笑话；0分-无关内容，不是笑话。请返回JSON格式：{\"score\": 8, \"feedback\": \"评价说明\"}",
                theme.getName(),
                theme.getPrompt() != null ? theme.getPrompt() : "无特殊说明",
                joke.getContent()
            );
            
            // 调用AI进行评分
            OpenAiChatModel chatModel = aiConfig.createOpenAiChatModel(apiKey, baseUrl, modelName);
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String aiResponse = response.getResult().getOutput().getText();
            
            // 解析AI返回的JSON结果
            String cleanedResponse = aiResponse.trim();
            // 移除可能的markdown代码块标记
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
            
            Map<String, Object> aiResult = objectMapper.readValue(cleanedResponse, new TypeReference<Map<String, Object>>() {});
            BigDecimal score = new BigDecimal(aiResult.get("score").toString());
            String feedback = (String) aiResult.get("feedback");
            
            // 保存评分结果
            joke.setAiScore(score);
            jokeRepository.save(joke);
            
            // 更新向量数据库
            vectorRepository.updateJoke(joke);
            
            Map<String, Object> result = new HashMap<>();
            result.put("jokeId", jokeId);
            Map<String, Object> aiScore = new HashMap<>();
            aiScore.put("score", score);
            aiScore.put("feedback", feedback);
            result.put("aiScore", aiScore);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("AI评分失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量AI评分
     */
    public Map<String, Object> aiBatchScore(Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> jokeIds = (List<String>) request.get("jokeIds");
            String apiKey = (String) request.get("apiKey");
            String modelName = (String) request.getOrDefault("modelName", "deepseek-chat");
            String baseUrl = (String) request.getOrDefault("baseUrl", "https://api.deepseek.com");
            
            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;
            
            for (String jokeId : jokeIds) {
                try {
                    Map<String, Object> singleRequest = new HashMap<>();
                    singleRequest.put("jokeId", jokeId);
                    singleRequest.put("apiKey", apiKey);
                    singleRequest.put("modelName", modelName);
                    singleRequest.put("baseUrl", baseUrl);
                    
                    Map<String, Object> singleResult = aiScore(singleRequest);
                    singleResult.put("success", true);
                    results.add(singleResult);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("jokeId", jokeId);
                    errorResult.put("success", false);
                    errorResult.put("error", e.getMessage());
                    results.add(errorResult);
                    failedCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", jokeIds.size());
            result.put("successCount", successCount);
            result.put("failedCount", failedCount);
            result.put("results", results);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("批量AI评分失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * AI智能生成笑话 - 基于RAG的LLM生成
     */
    public Map<String, Object> aiGenerate(Map<String, Object> request) {
        try {
            // 处理themeId的类型转换，支持Integer和String
            Object themeIdObj = request.get("themeId");
            Long themeId;
            if (themeIdObj instanceof Integer) {
                themeId = ((Integer) themeIdObj).longValue();
            } else if (themeIdObj instanceof String) {
                themeId = Long.valueOf((String) themeIdObj);
            } else {
                throw new RuntimeException("无效的themeId类型");
            }
            
            String prompt = (String) request.get("prompt");
            String apiKey = (String) request.get("apiKey");
            String modelName = (String) request.getOrDefault("modelName", "deepseek-chat");
            String baseUrl = (String) request.getOrDefault("baseUrl", "https://api.deepseek.com");
            
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                throw new RuntimeException("主题不存在");
            }
            
            Theme theme = themeOpt.get();
            
            // 记录AI生成请求的关键信息
            logger.info("AI生成笑话请求 - 主题: {}, 用户提示: {}", theme.getName(), prompt);
            
            // RAG：基于Pgvector搜索相似笑话作为参考
            List<Document> similarJokes = new ArrayList<>();
            String searchQuery = prompt != null ? prompt : theme.getName();
            
            try {
                // 优化后的召回数量：增加到8个以提高参考质量
                similarJokes = vectorRepository.searchSimilarJokesByTheme(searchQuery, themeId, 8);
                logger.info("RAG召回结果数量: {}", similarJokes.size());
            } catch (Exception e) {
                logger.warn("RAG搜索失败: {}", e.getMessage());
            }
            
            // 构建LLM生成prompt
            StringBuilder llmPrompt = new StringBuilder();
            llmPrompt.append(theme.getPrompt() != null ? theme.getPrompt() : theme.getName())
                    .append("，现在请你基于用户的提示词，生成一个笑话，有笑点。\n\n");
            
            llmPrompt.append("以下是一些方法：\n")
                    .append("- 改编经典文学作品、影视台词的笑话\n")
                    .append("- 包含游戏内梗和网络梗的内容\n")
                    .append("- 涉及谐音梗、创新的冷笑话\n\n");
            
            // 如果有RAG结果，添加最佳示例
            if (!similarJokes.isEmpty()) {
                Document bestMatch = similarJokes.get(0);
                String referenceContent = bestMatch.getText();
                llmPrompt.append("以下是一个示例：").append(referenceContent).append("\n\n");
            }
            
            llmPrompt.append("接下来请基于用户的提示词生成，只输出最终的笑话，不要输出其他内容。\n");
            llmPrompt.append("提示词：").append(prompt != null ? prompt : "无特殊要求");

            logger.info("LLM Prompt: {}", llmPrompt.toString());
            
            // 调用LLM生成笑话
            OpenAiChatModel chatModel = aiConfig.createOpenAiChatModel(apiKey, baseUrl, modelName);
            ChatResponse response = chatModel.call(new Prompt(llmPrompt.toString()));
            String generatedContent = response.getResult().getOutput().getText().trim();
            
            // 生成标题（简单规则）
            String generatedTitle = theme.getName() + "笑话";
            if (prompt != null && !prompt.trim().isEmpty()) {
                generatedTitle = prompt + "版" + theme.getName() + "笑话";
            }
            
            // 构建返回结果
            List<Map<String, Object>> generatedJokes = new ArrayList<>();
            Map<String, Object> jokeInfo = new HashMap<>();
            jokeInfo.put("title", generatedTitle);
            jokeInfo.put("content", generatedContent);
            jokeInfo.put("isAiGenerated", true);
            jokeInfo.put("aiScore", 7.5); // 默认评分
            jokeInfo.put("qualityLevel", "GOOD"); // 默认质量等级
            
            generatedJokes.add(jokeInfo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("jokes", generatedJokes);
            result.put("ragUsed", !similarJokes.isEmpty());
            result.put("referenceCount", similarJokes.size());
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("AI生成笑话失败: " + e.getMessage(), e);
        }
    }
}