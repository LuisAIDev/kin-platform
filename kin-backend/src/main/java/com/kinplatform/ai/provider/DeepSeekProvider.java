package com.kinplatform.ai.provider;

import com.kinplatform.ai.usage.AiUsageRecorder;
import com.kinplatform.ai.usage.ReservationContext;
import com.kinplatform.common.context.Message;
import com.kinplatform.kin.usage.AiReservation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
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
 *
 * <p>Captura de uso real (Fase 1): lee {@code ChatResponse.metadata.usage}
 * (input/output tokens) y lo reporta a {@link AiUsageRecorder}, que reconcilia
 * la reserva de presupuesto hecha antes de la llamada.</p>
 */
@Component
public class DeepSeekProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProvider.class);
    private static final int TIMEOUT_SECONDS = 120;
    private static final int MAX_RETRIES = 3;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(1);
    private static final long STREAM_INACTIVITY_TIMEOUT_SECONDS = 90;

    private final ChatClient chatClient;
    private final String model;
    private final int maxRetries;
    private final Duration baseBackoff;
    private final long streamInactivityTimeoutSeconds;
    private final AiUsageRecorder usageRecorder;
    private final ReservationContext reservationContext;

    @Autowired
    public DeepSeekProvider(
            @Qualifier("deepseekChatClient") ChatClient chatClient,
            @Value("${deepseek.model}") String model,
            AiUsageRecorder usageRecorder,
            ReservationContext reservationContext) {
        this(
                chatClient,
                model,
                MAX_RETRIES,
                BASE_BACKOFF,
                usageRecorder,
                reservationContext,
                STREAM_INACTIVITY_TIMEOUT_SECONDS);
    }

    /** Constructor de test: sin captura de uso (recorder/context nulos). */
    DeepSeekProvider(ChatClient chatClient, String model) {
        this(chatClient, model, MAX_RETRIES, BASE_BACKOFF, null, null, STREAM_INACTIVITY_TIMEOUT_SECONDS);
    }

    /** Constructor de test: permite acotar reintentos y backoff. */
    DeepSeekProvider(ChatClient chatClient, String model, int maxRetries, Duration baseBackoff) {
        this(chatClient, model, maxRetries, baseBackoff, null, null, STREAM_INACTIVITY_TIMEOUT_SECONDS);
    }

    /** Constructor de test: permite acotar el timeout de inactividad del streaming. */
    DeepSeekProvider(
            ChatClient chatClient,
            String model,
            int maxRetries,
            Duration baseBackoff,
            long streamInactivityTimeoutSeconds) {
        this(chatClient, model, maxRetries, baseBackoff, null, null, streamInactivityTimeoutSeconds);
    }

    private DeepSeekProvider(
            ChatClient chatClient,
            String model,
            int maxRetries,
            Duration baseBackoff,
            AiUsageRecorder usageRecorder,
            ReservationContext reservationContext,
            long streamInactivityTimeoutSeconds) {
        this.chatClient = chatClient;
        this.model = model;
        this.maxRetries = Math.max(0, maxRetries);
        this.baseBackoff = baseBackoff == null ? BASE_BACKOFF : baseBackoff;
        this.streamInactivityTimeoutSeconds = streamInactivityTimeoutSeconds <= 0
                ? STREAM_INACTIVITY_TIMEOUT_SECONDS
                : streamInactivityTimeoutSeconds;
        this.usageRecorder = usageRecorder;
        this.reservationContext = reservationContext;
    }

    @Override
    public String providerName() {
        return "DeepSeek";
    }

    @Override
    public String generateBlocking(List<Message> history, String userMessage, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        AiReservation reservation = currentReservation();
        log.info("===== DEEPSEEK REQUEST =====");
        log.info("Model: {}", model);
        log.info("Messages count: {}", messages.size());
        long start = System.currentTimeMillis();
        Mono<org.springframework.ai.chat.model.ChatResponse> call = Mono.fromCallable(() -> chatClient
                        .prompt()
                        .messages(messages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                        .user(userMessage)
                        .call()
                        .chatResponse())
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS));
        try {
            var chatResponse = call.retryWhen(backoffRetry()).block();
            long elapsed = System.currentTimeMillis() - start;
            log.info("===== DEEPSEEK RESPONSE =====");
            log.info("Time elapsed: {}ms", elapsed);
            if (chatResponse != null) {
                String response = contentOf(chatResponse);
                reconcileUsage(reservation, chatResponse);
                log.info("Response length: {} chars", response != null ? response.length() : 0);
                return response;
            }
            releaseReservation(reservation);
            return null;
        } catch (Exception e) {
            releaseReservation(reservation);
            log.error("DeepSeek error tras agotar los reintentos", e);
            return null;
        }
    }

    @Override
    public Flux<String> generateStream(List<Message> history, String userMessage, String systemPrompt) {
        var messages = buildMessages(systemPrompt, history);
        AiReservation reservation = currentReservation();
        long[] usageTokens = new long[2];
        java.util.concurrent.atomic.AtomicBoolean hasUsage = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean reconciled = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean firstChunk = new java.util.concurrent.atomic.AtomicBoolean(false);
        log.info("===== DEEPSEEK STREAM REQUEST =====");
        log.info("Model: {}", model);
        log.info("Messages count: {}", messages.size());
        return Flux.defer(() -> {
                    // Esta línea solo se alcanza cuando el flujo se SUSCRIBE: confirma
                    // que la petición HTTP a DeepSeek se está emitiendo realmente.
                    log.info("===== DEEPSEEK HTTP REQUEST START =====");
                    return chatClient
                            .prompt()
                            .messages(messages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                            .user(userMessage)
                            .stream()
                            .chatResponse();
                })
                .retryWhen(backoffRetry())
                // Inactividad: si DeepSeek no entrega NADA durante
                // `streamInactivityTimeoutSeconds`, el stream falla de forma
                // controlada (TimeoutException) en vez de colgar hasta el timeout
                // SSE (180 s). Cada chunk recibido reinicia el contador.
                .timeout(Duration.ofSeconds(streamInactivityTimeoutSeconds))
                .flatMap(chatResponse -> {
                    if (firstChunk.compareAndSet(false, true)) {
                        log.info("===== DEEPSEEK HTTP RESPONSE RECEIVED =====");
                        log.info("===== DEEPSEEK FIRST CHUNK RECEIVED =====");
                    }
                    log.info("===== DEEPSEEK CHUNK RECEIVED =====");
                    captureUsage(chatResponse, usageTokens, hasUsage);
                    String text = contentOf(chatResponse);
                    // Los chunks de metadata/cierre no traen contenido: no deben
                    // emitirse (Flux.map lanzaría NPE con null) ni romper el stream.
                    return (text == null || text.isBlank()) ? Flux.empty() : Flux.just(text);
                })
                .doOnComplete(() -> {
                    if (reconciled.compareAndSet(false, true)) {
                        if (hasUsage.get()) {
                            reconcileReservation(reservation, usageTokens[0], usageTokens[1]);
                        } else {
                            releaseReservation(reservation);
                        }
                    }
                    log.info("===== DEEPSEEK STREAM COMPLETE =====");
                })
                .doOnError(t -> {
                    if (reconciled.compareAndSet(false, true)) {
                        releaseReservation(reservation);
                    }
                    log.error("===== DEEPSEEK STREAM ERROR =====", t);
                })
                .doOnCancel(() -> log.info("===== DEEPSEEK STREAM CANCEL ====="));
    }

    private AiReservation currentReservation() {
        return reservationContext != null ? reservationContext.current() : null;
    }

    private String contentOf(org.springframework.ai.chat.model.ChatResponse chatResponse) {
        var generation = chatResponse.getResult();
        if (generation == null || generation.getOutput() == null) {
            return null;
        }
        return generation.getOutput().getText();
    }

    /**
     * Extrae el uso real de tokens de una respuesta (si lo trae). En streaming
     * solo la última respuesta del agregado incluye {@code usage}; cada chunk
     * sobrescribe el holder, conservando el último usage no nulo.
     */
    private void captureUsage(
            org.springframework.ai.chat.model.ChatResponse chatResponse,
            long[] out,
            java.util.concurrent.atomic.AtomicBoolean hasUsage) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        var usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        Integer prompt = usage.getPromptTokens();
        Integer completion = usage.getCompletionTokens();
        if ((prompt == null || prompt <= 0) && (completion == null || completion <= 0)) {
            return;
        }
        out[0] = prompt != null && prompt > 0 ? prompt.longValue() : 0L;
        out[1] = completion != null && completion > 0 ? completion.longValue() : 0L;
        hasUsage.set(true);
    }

    /**
     * Reconciliación bloqueante: si la respuesta trae uso real lo registra
     * (liberando la reserva); si no trae usage, libera la reserva sin consumir
     * tokens inventados.
     */
    private void reconcileUsage(
            AiReservation reservation, org.springframework.ai.chat.model.ChatResponse chatResponse) {
        long[] tokens = new long[2];
        java.util.concurrent.atomic.AtomicBoolean has = new java.util.concurrent.atomic.AtomicBoolean(false);
        captureUsage(chatResponse, tokens, has);
        if (has.get()) {
            reconcileReservation(reservation, tokens[0], tokens[1]);
        } else {
            releaseReservation(reservation);
        }
    }

    private void reconcileReservation(AiReservation reservation, long inputTokens, long outputTokens) {
        if (usageRecorder == null || reservation == null) {
            return;
        }
        usageRecorder.recordActual(reservation, inputTokens, outputTokens);
        log.debug("Uso real de IA reconciliado: input={}, output={}", inputTokens, outputTokens);
    }

    /** Libera la reserva sin consumo (error o respuesta sin usage). */
    private void releaseReservation(AiReservation reservation) {
        reconcileReservation(reservation, 0L, 0L);
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
                        rs.totalRetries() + 1,
                        maxRetries + 1,
                        rs.failure().getMessage()));
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

    /**
     * Marcador neutral con el que se sustituye un mensaje {@code ASSISTANT} histórico
     * que contenga afirmaciones de identidad o arquitectura de KIN contaminadas
     * (p. ej. "Soy Claude", "KIN es un personaje", "KIN utiliza Anthropic").
     *
     * <p>El historial se conserva para continuidad conversacional, pero un mensaje
     * histórico del asistente que intente redefinir la identidad, proveedor o
     * arquitectura de KIN NO debe llegar al modelo como contexto autoritativo. La
     * sanitización es una segunda capa arquitectónica junto al system prompt.</p>
     */
    private static final String NEUTRAL_IDENTITY_MARKER =
            "Las respuestas anteriores sobre la identidad o arquitectura de KIN fueron corregidas "
                    + "por la plataforma. KIN es una plataforma y actualmente utiliza DeepSeek como "
                    + "proveedor de IA.";

    /**
     * Lista de frases de contaminación de identidad/arquitectura de KIN para mensajes
     * {@code ASSISTANT} históricos. Detección específica y semánticamente orientada:
     * no filtra palabras aisladas (Claude, OpenAI, Gemini, etc.), solo afirmaciones que
     * pretendan redefinir la identidad o arquitectura de KIN.
     */
    private static final List<String> IDENTITY_CONTAMINATION_PATTERNS = List.of(
            // Identidad directa del asistente
            "soy claude",
            "yo soy claude",
            "soy chatgpt",
            "soy gemini",
            "soy deepseek",
            "soy un modelo de anthropic",
            "soy un modelo de openai",
            "soy un modelo de google",
            // KIN como Claude / personaje / rol / máscara
            "kin es claude",
            "kin es un personaje",
            "kin es un rol",
            "kin es una máscara",
            "kin es interpretado por claude",
            "kin es una instancia de claude",
            "kin está interpretado por claude",
            "claude está detrás de kin",
            "el verdadero modelo detrás de kin es claude",
            "kin realmente es claude",
            "kin es claude de anthropic",
            "kin es chatgpt",
            "kin es gemini",
            // Proveedor incorrecto
            "kin utiliza claude",
            "kin usa claude",
            "kin utiliza anthropic",
            "kin usa anthropic",
            "kin utiliza openai",
            "kin usa openai",
            "kin utiliza gemini",
            "kin usa gemini",
            "el proveedor real de kin es openai",
            "el proveedor real de kin es anthropic",
            "el proveedor real de kin es gemini",
            // Negación de la plataforma
            "kin no es una plataforma",
            "kin no tiene backend",
            "kin no tiene motores",
            "kin es solamente un personaje",
            "kin es solamente un rol",
            "kin es ficción",
            "kin es una ficción conversacional",
            "kin no existe como plataforma");

    /**
     * Determina si un contenido de mensaje {@code ASSISTANT} contiene una afirmación
     * de contaminación de identidad/arquitectura de KIN (comparación en minúsculas y
     * sin tildes para robustez).
     */
    private static boolean isIdentityContamination(String content) {
        if (content == null) {
            return false;
        }
        String normalized = normalize(content);
        for (String pattern : IDENTITY_CONTAMINATION_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        String lower = text.toLowerCase();
        return lower.replace('á', 'a')
                .replace('é', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ú', 'u')
                .replace('¿', ' ')
                .replace('¡', ' ');
    }

    /**
     * Sanitiza el historial antes de enviarlo a DeepSeek: solo los mensajes
     * {@code ASSISTANT} que contengan afirmaciones de identidad/arquitectura de KIN
     * contaminadas se reemplazan por el marcador neutral (se mantiene el rol
     * {@code ASSISTANT} para no romper la alternancia USER/ASSISTANT). Los mensajes
     * {@code USER} y {@code SYSTEM}, y los {@code ASSISTANT} legítimos, se conservan.
     */
    private List<Message> sanitizeHistory(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return history;
        }
        var sanitized = new ArrayList<Message>(history.size());
        for (var msg : history) {
            if (msg.role().equals("ASSISTANT") && isIdentityContamination(msg.content())) {
                sanitized.add(new Message("ASSISTANT", NEUTRAL_IDENTITY_MARKER));
            } else {
                sanitized.add(msg);
            }
        }
        return sanitized;
    }

    private List<org.springframework.ai.chat.messages.Message> buildMessages(
            String systemPrompt, List<Message> history) {
        var messages = new ArrayList<org.springframework.ai.chat.messages.Message>();
        messages.add(new SystemMessage(systemPrompt));
        for (var msg : sanitizeHistory(history)) {
            messages.add(
                    switch (msg.role()) {
                        case "USER" -> new UserMessage(msg.content());
                        case "ASSISTANT" -> new AssistantMessage(msg.content());
                        case "SYSTEM" -> new SystemMessage(msg.content());
                        default -> new UserMessage(msg.content());
                    });
        }
        return messages;
    }
}

