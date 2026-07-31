package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;

import java.util.ArrayList;

/**
 * Ensambla el resumen ejecutivo: identidad del proyecto, score global y
 * puntos destacados. Proyecta valores ya calculados; no calcula nada nuevo.
 */
public class ExecutiveSummaryAssembler implements SectionAssembler<ExecutiveSummary> {

    @Override
    public ExecutiveSummary assemble(ReportInput input) {
        var score = input.score();
        var evaluation = input.evaluation();
        var highlights = new ArrayList<>(score.strengths());
        input.opportunity().topOpportunities().stream()
            .limit(2)
            .forEach(opportunity -> highlights.add(opportunity.title()));
        return new ExecutiveSummary(
            input.projectTitle(),
            input.projectCategory(),
            score.totalScore(),
            score.maxScore(),
            score.viabilityLabel(),
            evaluation.coveragePercent() * 100,
            buildSummaryText(input, score),
            highlights);
    }

    private String buildSummaryText(ReportInput input, com.kinplatform.kin.scoring.ScoreResult score) {
        var evaluation = input.evaluation();
        return "El proyecto \u00AB" + input.projectTitle() + "\u00BB alcanza una cobertura del "
            + Math.round(evaluation.coveragePercent() * 100) + "% y un score de " + score.totalScore()
            + "/" + score.maxScore() + " (" + score.viabilityLabel() + "). Se detectaron "
            + input.recommendation().recommendations().size() + " recomendaciones, "
            + input.risk().risks().size() + " riesgos y "
            + input.opportunity().opportunities().size() + " oportunidades.";
    }
}
