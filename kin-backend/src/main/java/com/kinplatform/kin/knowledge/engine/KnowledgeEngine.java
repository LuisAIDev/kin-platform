package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeResult;

/**
 * Motor canonizado de adquisición de conocimiento externo (ADR-014, ADR-005/009).
 *
 * <p>Implementa {@link DomainEngine} (fase {@code KNOWLEDGE}, tipo {@code DOMAIN})
 * para integrarse con la infraestructura común de motores sin modificar su
 * lógica. Recibe un {@link KnowledgeInput}, consulta únicamente mediante el
 * {@link KnowledgeGateway} y produce exclusivamente un {@link KnowledgeResult}.</p>
 *
 * <p>El motor nunca habla con un LLM, nunca construye prompts y nunca depende de
 * infraestructura: la adquisición, validación, selección y cálculo de confianza y
 * calidad son decisiones deterministas de Java (principio "Java decide. El LLM
 * únicamente comunica.").</p>
 */
public class KnowledgeEngine implements DomainEngine<KnowledgeInput, KnowledgeResult> {

    public static final String GENERATOR_NAME = "KnowledgeEngine";
    public static final String ENGINE_VERSION = "v1";

    private final KnowledgeGateway gateway;

    public KnowledgeEngine(KnowledgeGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, ENGINE_VERSION, "KIN Architecture Team",
            EnginePhase.KNOWLEDGE, EngineType.DOMAIN, 50);
    }

    @Override
    public KnowledgeResult evaluate(KnowledgeInput input) {
        if (input == null || input.request() == null || gateway == null) {
            return KnowledgeResult.empty();
        }
        return gateway.acquire(input.request());
    }
}
