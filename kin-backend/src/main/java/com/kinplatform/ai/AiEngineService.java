package com.kinplatform.ai;

import com.kinplatform.ai.provider.ProviderRouter;
import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.context.Message;
import com.kinplatform.kin.context.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adaptador del puerto {@link AIResponder} sobre el router de proveedores de IA.
 *
 * <p>Es un adaptador de aplicación: la construcción del prompt ya no vive aquí
 * (fue extraída al {@link PromptAssembler} de dominio). Este servicio solo
 * enruta la petición al proveedor y aplica el fallback de indisponibilidad.</p>
 */
@Service
public class AiEngineService implements AIResponder {

    private static final Logger log = LoggerFactory.getLogger(AiEngineService.class);
    private static final String RATE_LIMIT_MESSAGE = "\u23F3 Hay mucho tr\u00E1fico en este momento. Por favor, intent\u00E1 de nuevo en unos segundos.";
    private static final String AI_UNAVAILABLE_MESSAGE = "Estoy teniendo dificultades temporales para conectarme con mi motor de IA. Por favor, intent\u00E1 de nuevo en unos segundos.";

    private final ProviderRouter providerRouter;
    private final PromptAssembler promptAssembler;

    public AiEngineService(ProviderRouter providerRouter) {
        this(providerRouter, new PromptAssembler());
    }

    @Autowired
    public AiEngineService(ProviderRouter providerRouter, PromptAssembler promptAssembler) {
        this.providerRouter = providerRouter;
        this.promptAssembler = promptAssembler;
    }

    @Override
    public String respond(AIRequest request) {
        log.info("=== CALLING AI PROVIDER === historySize={}",
                request.history() != null ? request.history().size() : 0);
        var response = providerRouter.routeBlocking(
                request.history(), request.userMessage(), request.systemPrompt());
        if (response == null || response.isBlank()) {
            log.warn("=== ALL PROVIDERS FAILED === returning unavailable message");
            return AI_UNAVAILABLE_MESSAGE;
        }
        return response;
    }

    @Override
    public Flux<String> respondStream(AIRequest request) {
        log.info("=== CALLING AI PROVIDER STREAM === historySize={}",
                request.history() != null ? request.history().size() : 0);
        return providerRouter.routeStream(
                request.history(), request.userMessage(), request.systemPrompt())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("=== STREAM ALL PROVIDERS FAILED ===");
                    return AI_UNAVAILABLE_MESSAGE;
                }));
    }

    public String generateAiResponse(List<Message> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory) {
        return generateAiResponse(history, userMessage, projectTitle, projectDescription, projectCategory, null);
    }

    public String generateAiResponse(List<Message> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory,
                                     ProjectContext context) {
        var systemPrompt = buildSystemPrompt(projectTitle, projectDescription, projectCategory, context);
        log.info("=== CALLING AI PROVIDER === historySize={}, contextDimensions={}",
                history.size(), context != null ? context.knownDimensionsCount() : 0);
        var response = providerRouter.routeBlocking(history, userMessage, systemPrompt);
        if (response == null || response.isBlank()) {
            log.warn("=== ALL PROVIDERS FAILED === returning unavailable message");
            return AI_UNAVAILABLE_MESSAGE;
        }
        return response;
    }

    public Flux<String> generateAiResponseStream(
            List<Message> history, String userMessage,
            String projectTitle, String projectDescription, String projectCategory) {
        return generateAiResponseStream(history, userMessage, projectTitle, projectDescription, projectCategory, null);
    }

    public Flux<String> generateAiResponseStream(
            List<Message> history, String userMessage,
            String projectTitle, String projectDescription, String projectCategory,
            ProjectContext context) {
        var systemPrompt = buildSystemPrompt(projectTitle, projectDescription, projectCategory, context);
        log.info("=== CALLING AI PROVIDER STREAM === historySize={}, contextDimensions={}",
                history.size(), context != null ? context.knownDimensionsCount() : 0);
        return providerRouter.routeStream(history, userMessage, systemPrompt)
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("=== STREAM ALL PROVIDERS FAILED ===");
                    return AI_UNAVAILABLE_MESSAGE;
                }));
    }

    public String buildSystemPrompt(String title, String description, String category,
                                     ProjectContext context) {
        return promptAssembler.assemble(title, description, category, context);
    }
}
