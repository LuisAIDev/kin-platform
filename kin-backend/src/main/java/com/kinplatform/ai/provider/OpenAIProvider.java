package com.kinplatform.ai.provider;

import com.kinplatform.kin.context.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OpenAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final int TIMEOUT_SECONDS = 120;

    private final ChatClient chatClient;
    private final String model;

    public OpenAIProvider(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.openai.chat.model}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.model = model;
    }

    @Override
    public String providerName() {
        return "OpenAI";
    }

    @Override
    public String generateBlocking(List<Message> history, String userMessage, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        log.info("===== OPENAI REQUEST =====");
        log.info("Model: {}", model);
        long start = System.currentTimeMillis();
        try {
            var future = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .messages(messages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                            .user(userMessage)
                            .call()
                            .content());
            var response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - start;
            log.info("===== OPENAI RESPONSE =====");
            log.info("Time elapsed: {}ms", elapsed);
            if (response != null) {
                log.info("Response length: {} chars", response.length());
            }
            return response;
        } catch (TimeoutException e) {
            log.error("OpenAI timeout after {}s", TIMEOUT_SECONDS, e);
            return null;
        } catch (Exception e) {
            log.error("OpenAI error", e);
            return null;
        }
    }

    @Override
    public Flux<String> generateStream(List<Message> history, String userMessage, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        log.info("===== OPENAI STREAM REQUEST =====");
        log.info("Model: {}", model);
        return chatClient.prompt()
                .messages(messages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                .user(userMessage)
                .stream()
                .content()
                .doOnComplete(() -> log.info("===== OPENAI STREAM COMPLETE ====="));
    }

    private List<org.springframework.ai.chat.messages.Message> buildMessages(String systemPrompt, List<Message> history) {
        var messages = new ArrayList<org.springframework.ai.chat.messages.Message>();
        messages.add(new SystemMessage(systemPrompt));
        for (var msg : history) {
            messages.add(switch (msg.role()) {
                case "USER" -> new UserMessage(msg.content());
                case "ASSISTANT" -> new AssistantMessage(msg.content());
                case "SYSTEM" -> new SystemMessage(msg.content());
                default -> new UserMessage(msg.content());
            });
        }
        return messages;
    }
}
