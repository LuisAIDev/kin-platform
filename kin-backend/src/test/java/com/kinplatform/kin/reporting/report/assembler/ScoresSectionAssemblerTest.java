package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoresSectionAssemblerTest {

    private final ScoresSectionAssembler assembler = new ScoresSectionAssembler();

    @Test
    void scores_deberiaProyectarScoreResult() {
        var scores = assembler.assemble(TestReportInputs.input());
        assertEquals(78, scores.totalScore());
        assertEquals(100, scores.maxScore());
        assertEquals(80, scores.categoryScores().get("Mercado"));
        assertEquals("VIABLE", scores.viabilityLabel());
        assertEquals(List.of("fortaleza 1"), scores.strengths());
        assertEquals(List.of("debilidad 1"), scores.weaknesses());
        assertEquals("v1", scores.scoringModelVersion());
        assertEquals(78.0, scores.confidenceLevel());
    }
}
