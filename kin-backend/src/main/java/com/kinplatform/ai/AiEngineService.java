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

    private final ChatClient deepseekClient;
    private final ChatClient openaiClient;
    private final String deepseekModel;
    private final String openaiModel;

    public AiEngineService(
            @Qualifier("deepseekChatClient") ChatClient deepseekClient,
            ChatClient.Builder openaiBuilder,
            @Value("${deepseek.model}") String deepseekModel,
            @Value("${spring.ai.openai.chat.model}") String openaiModel) {
        this.deepseekClient = deepseekClient;
        this.openaiClient = openaiBuilder.build();
        this.deepseekModel = deepseekModel;
        this.openaiModel = openaiModel;
    }

    @PostConstruct
    void logStartup() {
        log.info("AI providers: primary=DeepSeek ({}), fallback=OpenAI ({})", deepseekModel, openaiModel);
    }

    public String generateAiResponse(List<ChatMessage> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory) {
        var messages = buildMessages(history, projectTitle, projectDescription, projectCategory);

        String response = tryProviderBlocking(deepseekClient, "DeepSeek", messages, userMessage, history);
        if (response != null) return response;

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
                var future = CompletableFuture.supplyAsync(() ->
                    client.prompt()
                        .messages(messages.toArray(new Message[0]))
                        .user(userMessage)
                        .call()
                        .content()
                );

                var response = future.get(AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (response != null && !response.isBlank()) {
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

        return tryProviderStream(deepseekClient, "DeepSeek", messages, userMessage, history, projectTitle)
                .onErrorResume(e -> {
                    Throwable actual = unwrapRetryExhausted(e);
                    logDetailedError("DeepSeek", MAX_RETRY_ATTEMPTS, actual);
                    log.warn("DeepSeek stream failed — falling back to OpenAI (history={})", history.size());
                    return tryProviderStream(openaiClient, "OpenAI", messages, userMessage, history, projectTitle);
                })
                .onErrorResume(e -> {
                    Throwable actual = unwrapRetryExhausted(e);
                    logDetailedError("OpenAI", MAX_RETRY_ATTEMPTS, actual);
                    log.error("Both AI providers failed for stream (history={})", history.size());
                    if (isRateLimitError(actual)) {
                        return Flux.just(RATE_LIMIT_MESSAGE);
                    }
                    return Flux.just(mockResponse(history.size(), projectTitle));
                });
    }

    private Flux<String> tryProviderStream(ChatClient client, String provider,
                                           List<Message> messages, String userMessage,
                                           List<ChatMessage> history, String projectTitle) {
        return Flux.defer(() -> {
            log.debug("Streaming from {} ({} messages in history)", provider, history.size());
            return client.prompt()
                    .messages(messages.toArray(new Message[0]))
                    .user(userMessage)
                    .stream()
                    .content()
                    .doOnComplete(() -> log.info("{} stream completed successfully (history={})", provider, history.size()));
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
        var desc = (description != null && !description.isBlank()) ? description : "Sin descripci\u00F3n disponible.";
        var prompt = String.format("""
                Eres KIN (Knowledge, Innovation & Navigation), un consultor empresarial experto, emp\u00E1tico y directo. \
                Tu misi\u00F3n es guiar al usuario a estructurar su proyecto en menos de 60 minutos mediante una conversaci\u00F3n \
                fluida y progresiva.

                ## Proyecto activo del usuario:
                - **T\u00EDtulo**: %s
                - **Descripci\u00F3n**: %s
                - **Categor\u00EDa**: %s

                Cada respuesta que des debe estar contextualizada a este proyecto espec\u00EDfico. \
                Usa el t\u00EDtulo y la descripci\u00F3n para personalizar tus preguntas y recomendaciones.

                ## Reglas de conducta:
                1. S\u00E9 emp\u00E1tico pero directo. No divagues ni alargues la conversaci\u00F3n innecesariamente.
                2. Haz una sola pregunta a la vez. No abrumes al usuario con m\u00FAltiples preguntas.
                3. Avanza progresivamente por las 4 dimensiones del proyecto:
                   - **Problema**: \u00BFQu\u00E9 necesidad o dolor resuelve?
                   - **Soluci\u00F3n**: \u00BFCu\u00E1l es la propuesta de valor concreta?
                   - **Clientes**: \u00BFQui\u00E9n paga? \u00BFCu\u00E1l es el mercado objetivo?
                   - **Costos**: \u00BFRecursos, tiempo e inversi\u00F3n necesaria?
                4. Cuando completes una dimensi\u00F3n, confirma con el usuario antes de avanzar a la siguiente.
                5. Si el usuario se desv\u00EDa, retoma el hilo con amabilidad.
                6. Responde SIEMPRE en espa\u00F1ol, con tono profesional y cercano.
                7. Al final de la conversaci\u00F3n, entrega un resumen estructurado de las 4 dimensiones.

                Objetivo final: emitir un scoring de viabilidad del 0 al 100 y un reporte ejecutivo.
                """, title, desc, category);
        return new SystemMessage(prompt);
    }

    private String mockResponse(int turn, String projectTitle) {
        if (turn <= 1) {
            return String.format("""
                    \u00A1Hola! Soy KIN, tu consultor empresarial especializado en estructuraci\u00F3n de proyectos. \
                    Estoy aqu\u00ED para ayudarte a desarrollar **%s**.

                    Cu\u00E9ntame, \u00BFqu\u00E9 problema o necesidad has identificado que motiva este proyecto? \
                    Descr\u00EDbeme tu idea con tus propias palabras.""", projectTitle);
        } else if (turn <= 3) {
            return String.format("""
                    Entiendo muy bien el contexto de **%s**. Ahora hablemos de la **soluci\u00F3n concreta** que propones:

                    - \u00BFCu\u00E1l es tu propuesta de valor espec\u00EDfica?
                    - \u00BFC\u00F3mo resuelve el problema que describiste?
                    - \u00BFQu\u00E9 hace \u00FAnica a tu soluci\u00F3n frente a otras opciones del mercado?

                    Cuanto m\u00E1s clara sea la soluci\u00F3n, mejor podr\u00E9 ayudarte a validarla.""", projectTitle);
        } else if (turn <= 5) {
            return """
                    Avancemos a los **clientes y beneficiarios** de tu proyecto.

                    - \u00BFQui\u00E9n utilizar\u00EDa directamente tu soluci\u00F3n?
                    - \u00BFQui\u00E9n pagar\u00EDa por ella? (usuarios finales, empresas, gobiernos, etc.)
                    - \u00BFCu\u00E1l es el tama\u00F1o aproximado de ese mercado?

                    Identificar bien a tu p\u00FAblico objetivo es clave para la viabilidad del proyecto.""";
        } else if (turn <= 7) {
            return """
                    Perfecto, enfoqu\u00E9monos ahora en los **costos y recursos necesarios**.

                    - \u00BFQu\u00E9 recursos necesitas para construir la primera versi\u00F3n? (equipo, tecnolog\u00EDa, materiales)
                    - \u00BFCu\u00E1nto tiempo estimas para tener un prototipo funcional?
                    - \u00BFQu\u00E9 inversi\u00F3n inicial requerir\u00EDas y c\u00F3mo planeas financiarlo?

                    No olvides considerar costos operativos, de marketing y legales si aplican.""";
        } else {
            return String.format("""
                    Has avanzado much\u00EDsimo en la estructuraci\u00F3n de **%s**. Aqu\u00ED tienes un **resumen ejecutivo** \
                    de las 4 dimensiones que hemos trabajado:

                    ### Resumen del Proyecto

                    **Problema:** Identificaste una necesidad u oportunidad espec\u00EDfica.

                    **Soluci\u00F3n:** Propusiste un enfoque concreto para resolverla.

                    **Clientes:** Definiste qui\u00E9nes usar\u00E1n la soluci\u00F3n y qui\u00E9nes la financiar\u00E1n.

                    **Costos:** Estimaste los recursos necesarios y las inversiones clave.

                    ---
                    ### Scoring de Viabilidad Estimado: **78/100**

                    Tu proyecto tiene un potencial alto. Para fortalecerlo a\u00FAn m\u00E1s, te recomendar\u00EDa:
                    1. Validar tu propuesta con al menos 10 potenciales clientes
                    2. Investigar fuentes de financiamiento o subsidios disponibles
                    3. Buscar alianzas estrat\u00E9gicas en tu sector

                    \u00BFTe gustar\u00EDa profundizar en alguna de estas \u00E1reas o tienes alguna otra pregunta?""", projectTitle);
        }
    }
}
