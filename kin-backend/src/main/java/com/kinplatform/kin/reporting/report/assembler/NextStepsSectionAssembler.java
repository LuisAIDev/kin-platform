package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.ReportModel;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.NextStep;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ensambla los próximos pasos: agrega los top items de recomendaciones,
 * riesgos y oportunidades (top-3 por fuente) y los etiqueta como
 * {@link NextStep}, limitando al {@code nextStepsLimit} del modelo.
 */
public class NextStepsSectionAssembler implements SectionAssembler<NextStepsSection> {

    private static final int TOP_PER_SOURCE = 3;

    private final ReportModel model;

    public NextStepsSectionAssembler(ReportModel model) {
        this.model = model;
    }

    @Override
    public NextStepsSection assemble(ReportInput input) {
        var nextSteps = new ArrayList<NextStep>();
        nextSteps.addAll(topRecommendations(input.recommendation()));
        nextSteps.addAll(topRiskMitigations(input.risk()));
        nextSteps.addAll(topOpportunities(input.opportunity()));
        if (nextSteps.size() > model.nextStepsLimit()) {
            return new NextStepsSection(List.copyOf(nextSteps.subList(0, model.nextStepsLimit())));
        }
        return new NextStepsSection(nextSteps);
    }

    private List<NextStep> topRecommendations(RecommendationResult result) {
        return result.recommendations().stream()
            .sorted(Comparator.comparingInt(Recommendation::priority).reversed())
            .limit(TOP_PER_SOURCE)
            .map(rec -> NextStep.of(NextStep.SOURCE_RECOMMENDATION, rec.title(), rec.priority(),
                reason(rec)))
            .toList();
    }

    private List<NextStep> topRiskMitigations(RiskResult result) {
        return result.risks().stream()
            .sorted(Comparator.comparingInt(Risk::severityScore).reversed())
            .limit(TOP_PER_SOURCE)
            .map(risk -> NextStep.of(NextStep.SOURCE_RISK_MITIGATION, risk.title(),
                risk.severityScore(), reason(risk)))
            .toList();
    }

    private List<NextStep> topOpportunities(OpportunityResult result) {
        return result.opportunities().stream()
            .sorted(Comparator.comparingInt(Opportunity::priority).reversed())
            .limit(TOP_PER_SOURCE)
            .map(opp -> NextStep.of(NextStep.SOURCE_OPPORTUNITY, opp.title(), opp.priority(),
                reason(opp)))
            .toList();
    }

    private String reason(Recommendation rec) {
        return rec.explanation().appliedRule();
    }

    private String reason(Risk risk) {
        return risk.explanation().appliedRule();
    }

    private String reason(Opportunity opp) {
        return opp.explanation().appliedRule();
    }
}
