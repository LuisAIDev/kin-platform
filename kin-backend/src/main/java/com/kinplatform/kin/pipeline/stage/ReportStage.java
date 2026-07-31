package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.report.ReportEngine;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;

/**
 * Etapa del pipeline que ejecuta el {@link ReportEngine}.
 *
 * <p>Composición pura sobre {@link EngineStage} (mismo patrón que
 * {@code RiskStage}/{@code OpportunityStage}). Se ejecuta cuando los cuatro
 * resultados del reporte ya están presentes en el contexto.</p>
 */
public class ReportStage implements PipelineStage {

    private final EngineStage<ReportInput, ConsultingReport> delegate;

    public ReportStage(ReportEngine reportEngine) {
        this.delegate = new EngineStage<>(
            "Reporte",
            reportEngine,
            context -> context.projectContext() != null
                && context.evaluation() != null
                && context.decision() != null
                && context.decision().shouldGenerateReport()
                && context.scoreResult() != null
                && context.recommendationResult() != null
                && context.riskResult() != null
                && context.opportunityResult() != null,
            context -> new ReportInput(
                context.projectId(), context.projectTitle(), context.projectCategory(),
                context.projectContext(), context.evaluation(), context.decision(),
                context.scoreResult(), context.recommendationResult(),
                context.riskResult(), context.opportunityResult()),
            PipelineContext::consultingReport
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
