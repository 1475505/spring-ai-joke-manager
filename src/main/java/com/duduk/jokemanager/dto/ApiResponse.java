package com.duduk.jokemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一API响应格式
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API统一响应格式")
public class ApiResponse<T> {
    
    @Schema(description = "是否成功", example = "true")
    private boolean success;
    
    @Schema(description = "状态码", example = "200")
    private int code;
    
    @Schema(description = "消息", example = "操作成功")
    private String message;
    
    @Schema(description = "响应数据")
    private T data;
    
    @Schema(description = "错误信息")
    private ErrorDetail error;
    
    @Schema(description = "时间戳")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    
    @Schema(description = "请求ID")
    private String requestId;
    
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.requestId = UUID.randomUUID().toString();
    }
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = 200;
        response.message = "操作成功";
        response.data = data;
        return response;
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = 200;
        response.message = message;
        response.data = data;
        return response;
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        return response;
    }
    
    public static <T> ApiResponse<T> error(int code, String message, ErrorDetail error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        response.error = error;
        return response;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public ErrorDetail getError() { return error; }
    public void setError(ErrorDetail error) { this.error = error; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    /**
     * 错误详情
     */
    @Schema(description = "错误详情")
    public static class ErrorDetail {
        @Schema(description = "错误类型", example = "VALIDATION_ERROR")
        private String type;
        
        @Schema(description = "错误详细信息")
        private Object details;
        
        public ErrorDetail() {}
        
        public ErrorDetail(String type, Object details) {
            this.type = type;
            this.details = details;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
    }
}