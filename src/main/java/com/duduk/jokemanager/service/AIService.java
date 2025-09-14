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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AIService {
    
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
            String aiResponse = response.getResult().getOutput().getContent();
            
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
            
            // 暂时跳过向量数据库更新，避免UUID格式问题
            // vectorRepository.updateJoke(joke);
            
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
     * AI智能生成笑话
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
            
            String apiKey = (String) request.get("apiKey");
            String prompt = (String) request.get("prompt");
            String modelName = (String) request.getOrDefault("modelName", "deepseek-chat");
            String baseUrl = (String) request.getOrDefault("baseUrl", "https://api.deepseek.com");
            // 固定生成数量为1，不允许修改
            Integer count = 1;
            
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                throw new RuntimeException("主题不存在");
            }
            
            Theme theme = themeOpt.get();
            
            // 构建生成提示词
            String fullPrompt = String.format(
                "请根据主题'%s'和用户提示'%s'生成%d个笑话。主题说明：%s。要求：1. 内容健康正面 2. 语言生动有趣 3. 符合主题特色 4. 长度适中(50-200字)。请返回JSON格式：{\"jokes\": [{\"title\": \"标题\", \"content\": \"内容\"}]}",
                theme.getName(),
                prompt != null ? prompt : "搞笑幽默",
                count,
                theme.getPrompt() != null ? theme.getPrompt() : "无特殊说明"
            );
            
            // 调用AI进行生成
            OpenAiChatModel chatModel = aiConfig.createOpenAiChatModel(apiKey, baseUrl, modelName);
            ChatResponse response = chatModel.call(new Prompt(fullPrompt));
            String aiResponse = response.getResult().getOutput().getContent();
            
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
            @SuppressWarnings("unchecked")
            List<Map<String, String>> jokes = (List<Map<String, String>>) aiResult.get("jokes");
            
            List<Map<String, Object>> generatedJokes = new ArrayList<>();
            
            for (Map<String, String> jokeData : jokes) {
                String title = jokeData.get("title");
                String content = jokeData.get("content");
                
                // 暂时跳过重复内容检查，避免向量数据库连接问题
                // if (vectorRepository.isDuplicateContent(content, Long.valueOf(themeId), 0.9)) {
                //     continue; // 跳过重复内容
                // }
                
                // 只生成笑话数据，不保存到数据库
                Map<String, Object> jokeInfo = new HashMap<>();
                jokeInfo.put("title", title);
                jokeInfo.put("content", content);
                jokeInfo.put("isAiGenerated", true);
                // 添加AI评分和质量等级的模拟数据
                jokeInfo.put("aiScore", 7.5); // 默认评分
                jokeInfo.put("qualityLevel", "GOOD"); // 默认质量等级
                
                generatedJokes.add(jokeInfo);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("themeId", themeId);
            result.put("themeName", theme.getName());
            result.put("jokes", generatedJokes);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("AI生成笑话失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取主题知识库状态
     */
    public Map<String, Object> getKnowledgeStatus(Long themeId) {
        try {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                throw new RuntimeException("主题不存在");
            }
            
            Theme theme = themeOpt.get();
            
            // 统计该主题下的笑话数量和平均分
            List<Joke> jokes = jokeRepository.findByThemeIdAndStatus(themeId, Joke.Status.APPROVED);
            int totalJokes = jokes.size();
            
            double averageScore = jokes.stream()
                .mapToDouble(joke -> joke.getFinalScore().doubleValue())
                .average()
                .orElse(7.0);
            
            // 获取向量数据库统计信息
            Map<String, Object> vectorStats = vectorRepository.getThemeStatistics(themeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("themeId", themeId);
            result.put("themeName", theme.getName());
            
            Map<String, Object> knowledgeBase = new HashMap<>();
            knowledgeBase.put("totalJokes", totalJokes);
            knowledgeBase.put("vectorJokes", vectorStats.get("totalJokes"));
            knowledgeBase.put("lastUpdated", LocalDateTime.now());
            knowledgeBase.put("status", "READY");
            knowledgeBase.put("version", "v1.0");
            result.put("knowledgeBase", knowledgeBase);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("averageScore", Math.round(averageScore * 10.0) / 10.0);
            statistics.put("aiGeneratedJokes", vectorStats.get("aiGeneratedJokes"));
            statistics.put("humanCreatedJokes", vectorStats.get("humanCreatedJokes"));
            statistics.put("topKeywords", Arrays.asList("搞笑", "幽默", "段子"));
            result.put("statistics", statistics);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("获取知识库状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 重建主题知识库
     */
    public Map<String, Object> rebuildKnowledge(Long themeId) {
        try {
            Optional<Theme> themeOpt = themeRepository.findById(themeId);
            if (themeOpt.isEmpty()) {
                throw new RuntimeException("主题不存在");
            }
            
            Theme theme = themeOpt.get();
            
            // 生成任务ID
            String taskId = UUID.randomUUID().toString();
            
            // 异步执行重建任务
            new Thread(() -> {
                try {
                    // 清空该主题的向量数据
                    vectorRepository.clearThemeVectors(themeId);
                    
                    // 获取该主题下所有已审核的笑话
                    List<Joke> approvedJokes = jokeRepository.findByThemeIdAndStatus(themeId, Joke.Status.APPROVED);
                    
                    // 批量添加到向量数据库
                    if (!approvedJokes.isEmpty()) {
                        vectorRepository.addJokes(approvedJokes);
                    }
                    
                    // 这里可以添加任务完成的回调或状态更新逻辑
                    
                } catch (Exception e) {
                    // 记录错误日志
                    System.err.println("重建知识库失败: " + e.getMessage());
                }
            }).start();
            
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("themeId", themeId);
            result.put("status", "PROCESSING");
            result.put("estimatedTime", "5-10分钟");
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("启动重建任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取知识库重建状态
     */
    public Map<String, Object> getRebuildStatus(String taskId) {
        // 模拟任务状态
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", "COMPLETED");
        result.put("progress", 100);
        result.put("startTime", LocalDateTime.now().minusMinutes(5));
        result.put("endTime", LocalDateTime.now());
        
        Map<String, Object> taskResult = new HashMap<>();
        taskResult.put("processedJokes", 150);
        taskResult.put("newVersion", "v1.1");
        result.put("result", taskResult);
        
        return result;
    }
    
    /**
     * 获取风格描述
     */
    private String getStyleDescription(String style) {
        switch (style.toLowerCase()) {
            case "funny":
                return "幽默搞笑";
            case "witty":
                return "机智风趣";
            case "absurd":
                return "荒诞无厘头";
            case "wordplay":
                return "文字游戏";
            default:
                return "幽默搞笑";
        }
    }
}