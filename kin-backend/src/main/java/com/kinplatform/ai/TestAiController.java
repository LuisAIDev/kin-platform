package com.kinplatform.ai;

import com.kinplatform.common.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class TestAiController {

    private static final Logger log = LoggerFactory.getLogger(TestAiController.class);

    private final ChatClient deepseekClient;
    private final String deepseekModel;

    public TestAiController(
            @Qualifier("deepseekChatClient") ChatClient deepseekClient,
            @Value("${deepseek.model}") String deepseekModel
    ) {
        this.deepseekClient = deepseekClient;
        this.deepseekModel = deepseekModel;
    }

    @GetMapping("/deepseek")
    public ResponseEntity<Map<String, Object>> testDeepSeek() {
        log.info("===== TEST DEEPSEEK ENDPOINT CALLED =====");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("test", "DeepSeek direct test");
        result.put("model", deepseekModel);
        result.put("timestamp", java.time.Instant.now().toString());

        try {
            long startTime = System.currentTimeMillis();

            var future = CompletableFuture.supplyAsync(() ->
                deepseekClient.prompt()
                    .user("Hola, responde solo: OK funciono")
                    .call()
                    .content()
            );

            var response = future.get(60, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;

            result.put("status", "success");
            result.put("response_time_ms", elapsed);
            result.put("response", response);
            result.put("response_length", response != null ? response.length() : 0);

            log.info("===== TEST DEEPSEEK RESULT =====");
            log.info("Status: success, time: {}ms, response: {}", elapsed, response);
            log.info("================================");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            long elapsed = 0;
            result.put("status", "error");
            result.put("exception_class", e.getClass().getName());
            result.put("exception_message", e.getMessage() != null ? e.getMessage() : "null");

            Throwable cause = e.getCause();
            if (cause != null) {
                result.put("root_cause_class", cause.getClass().getName());
                result.put("root_cause_message", cause.getMessage() != null ? cause.getMessage() : "null");
            }

            log.error("===== TEST DEEPSEEK ERROR =====", e);
            log.error("================================");

            return ResponseEntity.ok(result);
        }
    }
}
