package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.scoring.ScoringEngine;
import com.kinplatform.kin.scoring.ScoringInput;
import com.kinplatform.kin.scoring.ScoreResult;

/**
 * Etapa del pipeline que ejecuta el {@link ScoringEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage}: mantiene la API pública
 * (constructor con el motor) y el comportamiento histórico mientras delega la
 * lógica genérica de ejecución al {@code EngineStage}.</p>
 */
public class ScoringStage implements PipelineStage {

    private final EngineStage<ScoringInput, ScoreResult> delegate;

    public ScoringStage(ScoringEngine scoringEngine) {
        this.delegate = new EngineStage<>(
            "Scoring",
            scoringEngine,
            context -> context.projectContext() != null
                && context.evaluation() != null
                && context.decision() != null
                && context.decision().shouldGenerateReport(),
            context -> new ScoringInput(
                context.projectContext(),
                context.evaluation()
            ),
            PipelineContext::scoreResult
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
