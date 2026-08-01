package com.kinplatform.ai;

import com.kinplatform.ai.provider.ProviderRouter;
import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador del puerto {@link AIResponder} sobre el router de proveedores de IA.
 *
 * <p>Recibe una petición ya resuelta y solo enruta la petición al proveedor,
 * aplicando el fallback de indisponibilidad.</p>
 */
@Service
public class AiEngineService implements AIResponder {

    private static final Logger log = LoggerFactory.getLogger(AiEngineService.class);
    private static final String AI_UNAVAILABLE_MESSAGE = "Estoy teniendo dificultades temporales para conectarme con mi motor de IA. Por favor, intentá de nuevo en unos segundos.";

    private final ProviderRouter providerRouter;

    public AiEngineService(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
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
}
