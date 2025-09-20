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
    

}