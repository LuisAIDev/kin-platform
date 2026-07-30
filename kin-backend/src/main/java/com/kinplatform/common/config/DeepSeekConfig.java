package com.kinplatform.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekConfig.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @PostConstruct
    void logConfig() {
        String masked = (apiKey != null && apiKey.length() > 5) ? apiKey.substring(0, 7) + "..." : "***";
        int keyLength = apiKey != null ? apiKey.length() : 0;
        log.info("DEEPSEEK_API_KEY presente: {}", apiKey != null && !apiKey.isBlank());
        log.info("DEEPSEEK_API_KEY longitud: {}", keyLength);
        if (apiKey != null && apiKey.length() >= 6) {
            log.info("DEEPSEEK_API_KEY primeros 6 chars: {}xxxxxx", apiKey.substring(0, 6));
        }
        log.info("DeepSeek configured with key: {}", masked);
    }

    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient(
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model) {
        log.info("===== DEEPSEEK CONFIG =====");
        log.info("Base URL: {}", baseUrl);
        log.info("Model: {}", model);
        log.info("API Key present: {}", apiKey != null && !apiKey.isBlank());
        log.info("API Key length: {}", apiKey != null ? apiKey.length() : 0);

        String fullUrl = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";
        log.info("Full chat completions URL: POST {}", fullUrl);

        var api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        var chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .build())
                .build();
        log.info("DeepSeek ChatClient created successfully");
        log.info("===== END DEEPSEEK CONFIG =====");
        return ChatClient.builder(chatModel).build();
    }
}
