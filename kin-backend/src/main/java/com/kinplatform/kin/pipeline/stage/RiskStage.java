package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.risk.RiskEngine;
import com.kinplatform.kin.reporting.risk.RiskInput;
import com.kinplatform.kin.reporting.risk.RiskResult;

/**
 * Etapa del pipeline que ejecuta el {@link RiskEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage}: mantiene la API pública
 * (constructor con el motor) y el comportamiento histórico mientras delega la
 * lógica genérica de ejecución al {@code EngineStage}.</p>
 */
public class RiskStage implements PipelineStage {

    private final EngineStage<RiskInput, RiskResult> delegate;

    public RiskStage(RiskEngine riskEngine) {
        this.delegate = new EngineStage<>(
            "Riesgos",
            riskEngine,
            context -> context.projectContext() != null
                && context.evaluation() != null
                && context.decision() != null
                && context.decision().shouldGenerateReport()
                && context.scoreResult() != null,
            context -> new RiskInput(
                context.projectContext(),
                context.evaluation(),
                context.decision(),
                context.scoreResult()
            ),
            PipelineContext::riskResult
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
