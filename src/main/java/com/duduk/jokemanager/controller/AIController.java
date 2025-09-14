package com.duduk.jokemanager.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.duduk.jokemanager.dto.ApiResponse;
import com.duduk.jokemanager.service.AIService;

@RestController
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    /**
     * AI智能评分
     */
    @PostMapping("/ai/score")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiScore(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = aiService.aiScore(request);
        return ResponseEntity.ok(ApiResponse.success(result, "AI评分完成"));
    }
    
    /**
     * 批量AI评分
     */
    @PostMapping("/ai/score/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiBatchScore(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = aiService.aiBatchScore(request);
        return ResponseEntity.ok(ApiResponse.success(result, "批量AI评分完成"));
    }
    
    /**
     * AI智能生成笑话
     */
    @PostMapping("/ai/generate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiGenerate(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = aiService.aiGenerate(request);
        return ResponseEntity.ok(ApiResponse.success(result, "AI生成笑话完成"));
    }
    
    /**
     * 获取主题知识库状态
     */
    @GetMapping("/ai/knowledge/{themeId}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKnowledgeStatus(@PathVariable Long themeId) {
        Map<String, Object> status = aiService.getKnowledgeStatus(themeId);
        return ResponseEntity.ok(ApiResponse.success(status, "获取知识库状态成功"));
    }
    
    /**
     * 重建主题知识库
     */
    @PostMapping("/ai/knowledge/{themeId}/rebuild")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ROOT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rebuildKnowledge(@PathVariable Long themeId) {
        Map<String, Object> task = aiService.rebuildKnowledge(themeId);
        return ResponseEntity.ok(ApiResponse.success(task, "知识库重建任务已启动"));
    }
    
    /**
     * 获取知识库重建状态
     */
    @GetMapping("/ai/knowledge/rebuild/{taskId}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRebuildStatus(@PathVariable String taskId) {
        Map<String, Object> status = aiService.getRebuildStatus(taskId);
        return ResponseEntity.ok(ApiResponse.success(status, "获取重建状态成功"));
    }
}