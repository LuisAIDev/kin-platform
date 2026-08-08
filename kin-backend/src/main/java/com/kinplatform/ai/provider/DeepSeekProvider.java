package com.kinplatform.ai.provider;

import com.kinplatform.kin.context.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Proveedor único de IA (DeepSeek). Implementa {@link AIProvider} y reemplaza
 * por completo al alias {@code OpenAIProvider} (que reutilizaba el mismo
 * {@code ChatClient} de DeepSeek): no existe fallback a OpenAI en ningún
 * punto del runtime.
 *
 * <p>Resiliencia (retry con backoff exponencial): ante errores transitorios
 * ({@code 429}, {@code 5xx}, timeout) reintenta la llamada con backoff
 * exponencial de Reactor ({@link Retry#backoff}). Si se agotan los reintentos,
 * devuelve {@code null} para que {@code AiEngineService} emita el mensaje de
 * indisponibilidad ("Estoy teniendo dificultades temporales..."). Las
 * respuestas vacías (200 con contenido nulo) NO se reintentan: se devuelven
 * tal cual.</p>
 */
@Component
public class DeepSeekProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProvider.class);
    private static final int TIMEOUT_SECONDS = 120;
    private static final int MAX_RETRIES = 3;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(1);

    private final ChatClient chatClient;
    private final String model;
    private final int maxRetries;
    private final Duration baseBackoff;

    @Autowired
    public DeepSeekProvider(
            @Qualifier("deepseekChatClient") ChatClient chatClient,
            @Value("${deepseek.model}") String model) {
        this(chatClient, model, MAX_RETRIES, BASE_BACKOFF);
    }

    /** Constructor de test: permite acotar reintentos y backoff. */
    DeepSeekProvider(ChatClient chatClient, String model, int maxRetries, Duration baseBackoff) {
        this.chatClient = chatClient;
        this.model = model;
        this.maxRetries = Math.max(0, maxRetries);
        this.baseBackoff = baseBackoff == null ? BASE_BACKOFF : baseBackoff;
    }

    @Override
    public String providerName() {
        return "DeepSeek";
    }

    @Override
    public String generateBlocking(List<Message> history, String userMessage, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        log.info("===== DEEPSEEK REQUEST =====");
        log.info("Model: {}", model);
        log.info("Messages count: {}", messages.size());
        long start = System.currentTimeMillis();
        Mono<String> call = Mono.fromCallable(() ->
                chatClient.prompt()
                        .messages(messages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                        .user(userMessage)
                        .call()
                        .content())
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS));
        try {
            String response = call.retryWhen(backoffRetry()).block();
            long elapsed = System.currentTimeMillis() - start;
            log.info("===== DEEPSEEK RESPONSE =====");
            log.info("Time elapsed: {}ms", elapsed);
            if (response != null) {
                log.info("Response length: {} chars", response.length());
            }
            return response;
        } catch (Exception e) {
            log.error("DeepSeek error tras agotar los reintentos", e);
            return null;
        }
    }

    @Override
    public Flux<String> generateStream(List<Message> history, String userMessage, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        log.info("===== DEEPSEEK STREAM REQUEST =====");
        log.info("Model: {}", model);
        log.info("Messages count: {}", messages.size());
        return Flux.defer(() -> chatClient.prompt()
                .messages(messages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                .user(userMessage)
                .stream()
                .content())
                .retryWhen(backoffRetry())
                .doOnComplete(() -> log.info("===== DEEPSEEK STREAM COMPLETE ====="));
    }

    /**
     * Estrategia de reintento con backoff exponencial (Reactor): reintenta solo
     * errores transitorios ({@code 429}/{@code 5xx}/timeout), con jitter.
     * {@code Retry.backoff(N, ...)} permite {@code N} reintentos tras el
     * intento inicial (N+1 intentos en total).
     */
    private Retry backoffRetry() {
        return Retry.backoff(maxRetries, baseBackoff)
                .filter(this::isTransient)
                .doBeforeRetry(rs -> log.warn(
                        "DeepSeek call falló (intento {}/{}): {} — reintentando con backoff",
                        rs.totalRetries() + 1, maxRetries + 1, rs.failure().getMessage()));
    }

    /**
     * Clasifica una excepción como transitoria (reintentable). Spring AI 1.1.x
     * lanza {@link TransientAiException} para {@code 5xx}, {@link
     * NonTransientAiException} para {@code 4xx} (con el status al inicio del
     * mensaje, p. ej. {@code "429 - ..."}); el path streaming puede emitir
     * {@link RestClientResponseException} ({@link
     * org.springframework.web.client.WebClientResponseException}) directamente.
     * Recorre la cadena de causas por robustez.
     */
    private boolean isTransient(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException || cause instanceof TransientAiException) {
                return true;
            }
            if (cause instanceof NonTransientAiException e) {
                return e.getMessage() != null && e.getMessage().startsWith("429 ");
            }
            if (cause instanceof RestClientResponseException e) {
                int code = e.getStatusCode().value();
                if (code == 429 || code >= 500) {
                    return true;
                }
            }
        }
        return false;
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
