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
        log.info("DeepSeek configured with key: {}", masked);
    }

    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient(
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model) {
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
        return ChatClient.builder(chatModel).build();
    }
}
