package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoresSectionTest {

    @Test
    void scores_deberiaProtegerListasYMapa() {
        var categoryScores = new HashMap<String, Integer>(Map.of("Mercado", 80));
        var strengths = new ArrayList<>(List.of("S"));
        var weaknesses = new ArrayList<>(List.of("W"));
        var scores = new ScoresSection(80, 100, categoryScores, "VIABLE", 80.0,
            strengths, weaknesses, "v1");

        categoryScores.put("Otro", 10);
        strengths.clear();
        weaknesses.clear();

        assertEquals(1, scores.categoryScores().size());
        assertEquals(1, scores.strengths().size());
        assertEquals(1, scores.weaknesses().size());
        assertThrows(UnsupportedOperationException.class,
            () -> scores.categoryScores().put("x", 1));
        assertThrows(UnsupportedOperationException.class,
            () -> scores.strengths().add("y"));
    }

    @Test
    void scores_deberiaAceptarNulos() {
        var scores = new ScoresSection(-5, -10, null, null, 200.0, null, null, null);
        assertEquals(0, scores.totalScore());
        assertEquals(0, scores.maxScore());
        assertTrue(scores.categoryScores().isEmpty());
        assertEquals("", scores.viabilityLabel());
        assertEquals(100.0, scores.confidenceLevel());
        assertTrue(scores.strengths().isEmpty());
        assertTrue(scores.weaknesses().isEmpty());
        assertEquals("", scores.scoringModelVersion());
    }

    @Test
    void scores_deberiaAcotarConfidenceLevel() {
        var scores = new ScoresSection(0, 0, Map.of(), "", -10.0, List.of(), List.of(), "");
        assertEquals(0.0, scores.confidenceLevel());
    }

    @Test
    void scores_deberiaExponerNombreYKind() {
        assertEquals("Scores", ScoresSection.empty().sectionName());
        assertEquals(ReportSectionKind.SCORING, ScoresSection.empty().kind());
    }

    @Test
    void scores_vacio_deberiaEstarVacio() {
        assertTrue(ScoresSection.empty().isEmpty());
        var scores = new ScoresSection(0, 100, Map.of("Mercado", 50), "", 0.0,
            List.of(), List.of(), "");
        assertFalse(scores.isEmpty());
    }
}
