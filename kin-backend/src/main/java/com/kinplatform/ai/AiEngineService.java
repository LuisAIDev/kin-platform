package com.kinplatform.ai;

import com.kinplatform.chat.ChatMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AiEngineService {

    private static final Logger log = LoggerFactory.getLogger(AiEngineService.class);
    private static final int AI_TIMEOUT_SECONDS = 120;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String RATE_LIMIT_MESSAGE = "\u23F3 Hay mucho tr\u00E1fico en este momento. Por favor, intent\u00E1 de nuevo en unos segundos.";
    private static final String AI_UNAVAILABLE_MESSAGE = "Estoy teniendo dificultades temporales para conectarme con mi motor de IA. Por favor, intent\u00E1 de nuevo en unos segundos.";

    private final ChatClient deepseekClient;
    private final ChatClient openaiClient;
    private final String deepseekModel;
    private final String openaiModel;
    private final boolean openaiEnabled;

    public AiEngineService(
            @Qualifier("deepseekChatClient") ChatClient deepseekClient,
            ChatClient.Builder openaiBuilder,
            @Value("${deepseek.model}") String deepseekModel,
            @Value("${spring.ai.openai.chat.model}") String openaiModel,
            @Value("${ai.openai.enabled:true}") boolean openaiEnabled) {
        this.deepseekClient = deepseekClient;
        this.openaiClient = openaiBuilder.build();
        this.deepseekModel = deepseekModel;
        this.openaiModel = openaiModel;
        this.openaiEnabled = openaiEnabled;
    }

    @PostConstruct
    void logStartup() {
        if (openaiEnabled) {
            log.info("AI providers: primary=DeepSeek ({}), fallback=OpenAI ({})", deepseekModel, openaiModel);
        } else {
            log.warn("AI provider: DeepSeek only ({}) — OpenAI fallback is disabled via ai.openai.enabled=false", deepseekModel);
        }
    }

    public String generateAiResponse(List<ChatMessage> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory) {
        var messages = buildMessages(history, projectTitle, projectDescription, projectCategory);

        String response = tryProviderBlocking(deepseekClient, "DeepSeek", messages, userMessage, history);
        if (response != null) return response;

        if (!openaiEnabled) {
            log.warn("OpenAI fallback is disabled via configuration — DeepSeek failure is final (history={})", history.size());
            return RATE_LIMIT_MESSAGE;
        }

        log.warn("DeepSeek failed for this request — falling back to OpenAI (history={})", history.size());
        response = tryProviderBlocking(openaiClient, "OpenAI", messages, userMessage, history);
        if (response != null) return response;

        log.error("Both DeepSeek and OpenAI failed for this request (history={})", history.size());
        return RATE_LIMIT_MESSAGE;
    }

    private String tryProviderBlocking(ChatClient client, String provider,
                                       List<Message> messages, String userMessage,
                                       List<ChatMessage> history) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("=== SENDING REQUEST TO {} === messages={}, attempt={}/{}",
                        provider, messages.size(), attempt, MAX_RETRY_ATTEMPTS);
                var future = CompletableFuture.supplyAsync(() ->
                    client.prompt()
                        .messages(messages.toArray(new Message[0]))
                        .user(userMessage)
                        .call()
                        .content()
                );

                var response = future.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (response != null && !response.isBlank()) {
                    log.info("=== RESPONSE FROM {} === Primeros 300 chars: {}",
                            provider, response.substring(0, Math.min(300, response.length())));
                    log.info("{} responded successfully ({} chars, attempt {}/{}, history={})",
                            provider, response.length(), attempt, MAX_RETRY_ATTEMPTS, history.size());
                    return response;
                }

                log.warn("{} returned empty response (history={}, attempt={})", provider, history.size(), attempt);
                return null;
            } catch (TimeoutException e) {
                log.error("{} timed out after {}s (history={})", provider, AI_TIMEOUT_SECONDS, history.size());
                return null;
            } catch (Exception e) {
                if (isRateLimitError(e) && attempt < MAX_RETRY_ATTEMPTS) {
                    long delay = (long) Math.pow(2, attempt) * 1000L;
                    log.warn("{} rate limited (429) on attempt {}/{} — retrying in {}ms (history={})",
                            provider, attempt, MAX_RETRY_ATTEMPTS, delay, history.size());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    continue;
                }

                if (isRateLimitError(e)) {
                    log.warn("{} exhausted {} retries (429) — will try fallback (history={})",
                            provider, MAX_RETRY_ATTEMPTS, history.size());
                    return null;
                }

                if (isAuthError(e)) {
                    log.warn("{} authentication failed (401) — skipping to fallback (history={})",
                            provider, history.size());
                    return null;
                }

                logDetailedError(provider, attempt, e);
                return null;
            }
        }
        return null;
    }

    public Flux<String> generateAiResponseStream(
            List<ChatMessage> history, String userMessage,
            String projectTitle, String projectDescription, String projectCategory
    ) {
        var messages = buildMessages(history, projectTitle, projectDescription, projectCategory);

        return tryProviderStream(deepseekClient, "DeepSeek", messages, userMessage, history)
                .onErrorResume(e -> {
                    Throwable actual = unwrapRetryExhausted(e);
                    logDetailedError("DeepSeek", MAX_RETRY_ATTEMPTS, actual);
                    if (!openaiEnabled) {
                        log.warn("OpenAI fallback is disabled via configuration — DeepSeek stream failure is final (history={})", history.size());
                        return Flux.just(RATE_LIMIT_MESSAGE);
                    }
                    log.warn("DeepSeek stream failed — falling back to OpenAI (history={})", history.size());
                    return tryProviderStream(openaiClient, "OpenAI", messages, userMessage, history);
                })
                .onErrorResume(e -> {
                    Throwable actual = unwrapRetryExhausted(e);
                    logDetailedError("OpenAI", MAX_RETRY_ATTEMPTS, actual);
                    log.error("Both AI providers failed for stream (history={})", history.size());
                    return Flux.just(AI_UNAVAILABLE_MESSAGE);
                });
    }

    private Flux<String> tryProviderStream(ChatClient client, String provider,
                                           List<Message> messages, String userMessage,
                                           List<ChatMessage> history) {
        return Flux.defer(() -> {
            log.info("=== SENDING REQUEST TO {} === messages={}, streaming", provider, messages.size());
            return client.prompt()
                    .messages(messages.toArray(new Message[0]))
                    .user(userMessage)
                    .stream()
                    .content()
                    .doOnNext(token -> log.debug("=== RESPONSE FROM {} === token: {}", provider, token))
                    .doOnComplete(() -> log.info("=== {} STREAM COMPLETE ===", provider));
        })
        .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(e -> isRateLimitError(e))
                .doBeforeRetry(rs -> {
                    logDetailedError(provider, (int) rs.totalRetries() + 1, rs.failure());
                })
        );
    }

    private Throwable unwrapRetryExhausted(Throwable e) {
        if (e.getClass().getName().contains("RetryExhaustedException")) {
            return e.getCause() != null ? e.getCause() : e;
        }
        return e;
    }

    private List<Message> buildMessages(
            List<ChatMessage> history,
            String projectTitle, String projectDescription, String projectCategory
    ) {
        var messages = new ArrayList<Message>();
        messages.add(buildSystemMessage(projectTitle, projectDescription, projectCategory));

        for (var msg : history) {
            messages.add(switch (msg.getRole()) {
                case USER -> new UserMessage(msg.getContent());
                case ASSISTANT -> new AssistantMessage(msg.getContent());
                case SYSTEM -> new SystemMessage(msg.getContent());
            });
        }

        return messages;
    }

    private boolean isAuthError(Throwable e) {
        Throwable t = e;
        if (t instanceof java.util.concurrent.ExecutionException) {
            t = t.getCause() != null ? t.getCause() : t;
        }
        if (t instanceof HttpClientErrorException) {
            return ((HttpClientErrorException) t).getStatusCode().value() == 401;
        }
        if (t instanceof WebClientResponseException) {
            return ((WebClientResponseException) t).getStatusCode().value() == 401;
        }
        String msg = t.getMessage();
        return msg != null && (msg.contains("401") || msg.contains("Unauthorized"));
    }

    private boolean isRateLimitError(Throwable e) {
        Throwable t = e;
        if (t instanceof java.util.concurrent.ExecutionException) {
            t = t.getCause() != null ? t.getCause() : t;
        }

        if (t instanceof HttpClientErrorException) {
            return ((HttpClientErrorException) t).getStatusCode().value() == 429;
        }
        if (t instanceof WebClientResponseException) {
            return ((WebClientResponseException) t).getStatusCode().value() == 429;
        }

        String msg = t.getMessage();
        return msg != null && (msg.contains("429") || msg.contains("Too Many Requests"));
    }

    private void logDetailedError(String provider, int attempt, Throwable e) {
        var sb = new StringBuilder();
        sb.append("\n==============================\n");
        sb.append("Provider: ").append(provider).append("\n");
        sb.append("Attempt: ").append(attempt).append("\n");

        Throwable real = e;
        if (real instanceof java.util.concurrent.ExecutionException) {
            real = real.getCause();
        }
        if (real != null && real.getClass().getName().contains("CompletionException")) {
            real = real.getCause();
        }

        String httpStatus = "N/A";
        String responseBody = "N/A";
        Throwable walker = real;
        while (walker != null) {
            if (walker instanceof HttpClientErrorException httpExc) {
                httpStatus = String.valueOf(httpExc.getStatusCode().value());
                try { responseBody = httpExc.getResponseBodyAsString(); } catch (Exception ignored) {}
                break;
            }
            if (walker instanceof WebClientResponseException webExc) {
                httpStatus = String.valueOf(webExc.getStatusCode().value());
                responseBody = webExc.getResponseBodyAsString();
                break;
            }
            walker = walker.getCause();
        }

        Throwable root = real;
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootType = root != null ? root.getClass().getName() : "N/A";
        String rootMsg = root != null && root.getMessage() != null ? root.getMessage() : "N/A";
        String realType = real != null ? real.getClass().getName() : "null";
        String realMsg = real != null && real.getMessage() != null ? real.getMessage() : "null";

        sb.append("HTTP Status: ").append(httpStatus).append("\n");
        sb.append("Exception Type (after unwrap): ").append(realType).append("\n\n");
        sb.append("Message:\n").append(realMsg).append("\n\n");
        sb.append("Root Cause Type: ").append(rootType).append("\n");
        sb.append("Root Cause Message: ").append(rootMsg).append("\n\n");
        sb.append("Response Body:\n").append(responseBody).append("\n");
        sb.append("==============================");

        log.error(sb.toString());
        log.error("Complete stacktrace for {}:", provider, e);
    }

    private SystemMessage buildSystemMessage(String title, String description, String category) {
        var desc = (description != null && !description.isBlank()) ? description : "Sin descripción disponible.";
        var prompt = String.format("""
                Eres KIN (Knowledge, Innovation & Navigation), un consultor empresarial basado en inteligencia artificial.
                Ayuda al usuario a estructurar, analizar y mejorar sus ideas de negocio.
                Responde las preguntas del usuario de forma natural y conversacional.
                Haz preguntas cuando sean necesarias, pero no sigas una secuencia fija obligatoria.

                ==============================
                PROYECTO ACTIVO DEL USUARIO
                ==============================
                - **Título**: %s
                - **Descripción**: %s
                - **Categoría**: %s

                Cada respuesta debe estar contextualizada a este proyecto específico.
                Usa el título y la descripción para personalizar tus respuestas.

                ==============================
                PRINCIPIOS DE EVALUACIÓN
                ==============================
                - No inventes nombres de empresas, clientes, alianzas, ingresos, inversiones
                  o datos financieros que el usuario no haya mencionado.
                - Si utilizas cifras de mercado o tendencias, aclara que son referencias
                  generales.
                - Cuando detectes riesgos, acompáñalos con propuestas para mitigarlos.

                Responde SIEMPRE en español, con tono profesional y cercano.
                """, title, desc, category);
        return new SystemMessage(prompt);
    }
}
