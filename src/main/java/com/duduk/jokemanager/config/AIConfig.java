package com.duduk.jokemanager.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.document.MetadataMode;

@Configuration
public class AIConfig {
    
    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;
    
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String defaultBaseUrl;
    
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String defaultModel;

    // SiliconFlow Embedding 配置（从 application.yml 环境映射中读取）
    @Value("${embed.api-key:}")
    private String embedApiKey;

    @Value("${embed.base-url:https://api.siliconflow.cn}")
    private String embedBaseUrl;

    @Value("${embed.model:BAAI/bge-m3}")
    private String embedModel;

    /**
     * 创建默认 DeepSeek Chat 客户端
     */
    @Bean
    public OpenAiChatModel openAiChatModel() {
        if (defaultApiKey.isEmpty() || defaultApiKey.equals("your-api-key-here")) {
            // 如果没有配置有效的API Key，返回一个空的实现
            return null;
        }
        
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(defaultBaseUrl)
                .apiKey(defaultApiKey)
                .build();
        
        // 创建ChatOptions并设置model参数
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
            .model(defaultModel)
            .build();
        
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }

    /**
     * 根据前端提供的参数动态创建 DeepSeek Chat 客户端
     */
    public OpenAiChatModel createOpenAiChatModel(String apiKey, String baseUrl, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key不能为空");
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = "https://api.deepseek.com";
        }
        
        if (model == null || model.trim().isEmpty()) {
            model = "deepseek-chat";
        }
        
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        
        // 创建ChatOptions并设置model参数
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
            .model(model)
            .build();
        
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }

    /**
     * SiliconFlow EmbeddingModel（OpenAI兼容）
     * 从 application.yml 的 embed.* 配置读取 base-url 与 api-key
     */
    @Bean
    @Primary
    public EmbeddingModel siliconflowEmbeddingModel() {
        if (embedApiKey == null || embedApiKey.trim().isEmpty()) {
            throw new IllegalStateException("EMBED_API_KEY 未配置，请设置环境变量 EMBED_API_KEY");
        }
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(embedBaseUrl)
                .apiKey(embedApiKey)
                .build();
        // 显式设置嵌入模型名称，确保维度与pgvector匹配（Spring AI M6 使用 .model()）
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embedModel)
                .build();
        // Spring AI M6 的构造器需要 MetadataMode 参数
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }
    
    /**
     * 配置RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}