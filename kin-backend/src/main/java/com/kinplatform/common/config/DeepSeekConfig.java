package com.kinplatform.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del proveedor DeepSeek.
 *
 * <p>Expone el {@link ChatModel} de DeepSeek como bean único (remediación C2):
 * al existir un {@code ChatModel}, el autoconfig de OpenAI de Spring AI se
 * retira ({@code @ConditionalOnMissingBean}) y el arranque deja de exigir
 * {@code OPENAI_API_KEY}. El {@code ChatClient} de DeepSeek y el
 * {@code ChatClient.Builder} (que usa {@code OpenAIProvider} como fallback)
 * se construyen sobre ese mismo modelo.</p>
 */
@Configuration
public class DeepSeekConfig {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekConfig.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @PostConstruct
    void logConfig() {
        log.info("DEEPSEEK_API_KEY presente: {}", apiKey != null && !apiKey.isBlank());
        log.info("DEEPSEEK_API_KEY longitud: {}", apiKey != null ? apiKey.length() : 0);
    }

    @Bean("deepseekChatModel")
    public ChatModel deepSeekChatModel(
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model) {
        log.info("===== DEEPSEEK CONFIG =====");
        log.info("Base URL: {}", baseUrl);
        log.info("Model: {}", model);
        log.info("API Key present: {}", apiKey != null && !apiKey.isBlank());

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
        return chatModel;
    }

    @Bean("deepseekChatClient")
    public ChatClient deepseekChatClient(@Qualifier("deepseekChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
