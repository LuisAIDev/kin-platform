package com.kinplatform.platform.reporting.report.assembler;

import com.kinplatform.platform.reporting.report.ReportInput;
import com.kinplatform.platform.reporting.report.SectionAssembler;
import com.kinplatform.platform.reporting.report.model.ScoresSection;

/**
 * Ensambla la sección de puntuación: proyección directa del
 * {@code ScoreResult} ya calculado.
 */
public class ScoresSectionAssembler implements SectionAssembler<ScoresSection> {

    @Override
    public ScoresSection assemble(ReportInput input) {
        var score = input.score();
        return new ScoresSection(
            score.totalScore(),
            score.maxScore(),
            score.categoryScores(),
            score.viabilityLabel(),
            score.confidence() * 100,
            score.strengths(),
            score.weaknesses(),
            score.engineVersion());
    }
}


