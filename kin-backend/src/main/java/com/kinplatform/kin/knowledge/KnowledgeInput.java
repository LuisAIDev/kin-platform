package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada canonizada del motor de conocimiento (ADR-014, ADR-005/009).
 *
 * <p>Envuelve la {@link KnowledgeRequest} que Java construyó para el turno.
 * Implementa {@link EngineInput} para que {@code KnowledgeEngine} (etapas
 * posteriores) sea ejecutable por la infraestructura común de motores.</p>
 */
public record KnowledgeInput(
    KnowledgeRequest request
) implements EngineInput {

    public KnowledgeInput {
        request = request == null ? KnowledgeRequest.empty() : request;
    }

    public static KnowledgeInput of(KnowledgeRequest request) {
        return new KnowledgeInput(request);
    }
}
