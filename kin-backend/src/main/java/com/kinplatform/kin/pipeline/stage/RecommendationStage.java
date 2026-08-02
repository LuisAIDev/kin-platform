package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.RecommendationEngine;
import com.kinplatform.kin.reporting.RecommendationInput;
import com.kinplatform.kin.reporting.RecommendationResult;

/**
 * Etapa del pipeline que ejecuta el {@link RecommendationEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage}: mantiene la API pública
 * (constructor con el motor) y el comportamiento histórico mientras delega la
 * lógica genérica de ejecución al {@code EngineStage}.</p>
 */
public class RecommendationStage implements PipelineStage {

    private final EngineStage<RecommendationInput, RecommendationResult> delegate;

    public RecommendationStage(RecommendationEngine recommendationEngine) {
        this.delegate = new EngineStage<>(
            "Recomendaciones",
            recommendationEngine,
            context -> context.projectContext() != null
                && context.evaluation() != null
                && context.decision() != null
                && context.decision().shouldGenerateReport()
                && context.scoreResult() != null,
            context -> {
                var input = new RecommendationInput(
                    context.projectContext(),
                    context.evaluation(),
                    context.decision(),
                    context.scoreResult()
                );
                var enrichment = context.enrichmentResult();
                return enrichment == null ? input : input.withEnrichment(enrichment);
            },
            PipelineContext::recommendationResult
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
