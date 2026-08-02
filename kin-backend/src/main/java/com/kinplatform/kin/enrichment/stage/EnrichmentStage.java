package com.kinplatform.kin.enrichment.stage;

import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.enrichment.EnrichmentInput;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.pipeline.stage.EngineStage;

/**
 * Etapa de enriquecimiento del pipeline (ADR-016, Etapa E6): ejecuta el
 * {@link EnrichmentEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage} (mismo patrón que
 * {@code KnowledgeStage}/{@code InterviewStage}): lee únicamente el
 * {@link com.kinplatform.kin.context.ProjectContext} y el
 * {@link com.kinplatform.kin.knowledge.KnowledgeResult} del
 * {@link PipelineContext}, construye el {@link EnrichmentInput}, invoca el
 * motor y almacena el {@link EnrichmentResult} en
 * {@code PipelineContext.enrichmentResult} (campo aditivo sancionado por
 * ADR-016).</p>
 *
 * <p>Offline-first: sin hechos verificados (o sin {@code knowledgeResult})
 * el motor degrada a {@link EnrichmentResult#empty()} y el pipeline se
 * comporta exactamente como antes de la Fase 8.</p>
 *
 * <p>La etapa nunca habla con APIs, Internet, HTTP, Spring, el LLM ni ningún
 * adaptador: la selección, ponderación y ranking de la evidencia son
 * decisiones deterministas de Java dentro del motor (principio "Java decide.
 * El LLM únicamente comunica.").</p>
 */
public class EnrichmentStage implements PipelineStage {

    private final EngineStage<EnrichmentInput, EnrichmentResult> delegate;

    public EnrichmentStage(EnrichmentEngine enrichmentEngine) {
        this.delegate = new EngineStage<>(
            "Enriquecimiento",
            enrichmentEngine,
            context -> context != null && context.projectContext() != null,
            context -> EnrichmentInput.of(context.projectContext(), context.knowledgeResult()),
            PipelineContext::withEnrichmentResult
        );
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public boolean supports(PipelineContext context) {
        return delegate.supports(context);
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        return delegate.execute(context);
    }
}
