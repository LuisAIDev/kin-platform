package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.opportunity.OpportunityEngine;
import com.kinplatform.kin.reporting.opportunity.OpportunityInput;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;

/**
 * Etapa del pipeline que ejecuta el {@link OpportunityEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage}: delega la lógica genérica de
 * ejecución al {@code EngineStage} manteniendo la API pública (constructor con
 * el motor) y el patrón histórico de las etapas de motores.</p>
 */
public class OpportunityStage implements PipelineStage {

    private final EngineStage<OpportunityInput, OpportunityResult> delegate;

    public OpportunityStage(OpportunityEngine opportunityEngine) {
        this.delegate = new EngineStage<>(
            "Oportunidades",
            opportunityEngine,
            context -> context.projectContext() != null
                && context.evaluation() != null
                && context.decision() != null
                && context.decision().shouldGenerateReport()
                && context.scoreResult() != null,
            context -> new OpportunityInput(
                context.projectContext(),
                context.evaluation(),
                context.decision(),
                context.scoreResult()
            ),
            PipelineContext::opportunityResult
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
