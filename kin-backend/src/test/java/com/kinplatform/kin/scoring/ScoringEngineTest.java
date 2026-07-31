package com.kinplatform.kin.scoring;

import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine(ScoringModel.defaultModel());

    @Test
    void metadata_deberiaSerUnMotorDeDominioDeScoring() {
        var metadata = engine.metadata();
        assertEquals("ScoringEngine", metadata.name());
        assertEquals(EnginePhase.SCORING, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals(30, metadata.priority());
        assertEquals("v1", metadata.version());
    }

    @Test
    void evaluate_deberiaPuntuarDimensionesCubiertas() {
        var result = engine.evaluate(richContext(), richEvaluation());

        assertTrue(result.totalScore() > 0);
        assertFalse(result.categoryScores().isEmpty());
        assertFalse(result.strengths().isEmpty());
        assertNotNull(result.explanation());
    }

    @Test
    void evaluate_deberiaSerMuyAlta_cuandoScoreYCoberturaSonAltos() {
        var result = engine.evaluate(richContext(), richEvaluation());
        assertEquals("MUY_ALTA", result.viabilityLabel());
    }

    @Test
    void evaluate_conEntradaNull_deberiaDevolverVacio() {
        assertTrue(engine.evaluate((ScoringInput) null).isEmpty());
        assertTrue(engine.evaluate(new ScoringInput(null, null)).isEmpty());
        assertTrue(engine.evaluate(new ScoringInput(new ProjectContext(), null)).isEmpty());
    }

    @Test
    void evaluate_conCoberturaBaja_deberiaSerBaja() {
        var context = ProjectContext.fromProject("X", "d", "c");
        var evaluation = new CompletenessEvaluation(
            0.1, List.of(), List.of(), 0.1,
            CompletenessEvaluation.MaturityLevel.EARLY,
            CompletenessEvaluation.ViabilityLevel.LOW, 0.1,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.EXPLORE_MORE,
            false, 2, AnalyzedDimension.values().length);
        var result = engine.evaluate(context, evaluation);
        assertEquals("BAJA", result.viabilityLabel());
    }

    @Test
    void scoreResult_deberiaCumplirElContratoEngineResult() {
        var result = engine.evaluate(richContext(), richEvaluation());
        assertTrue(result.confidence() > 0);
        assertEquals("ScoringEngine", result.generatedBy());
        assertEquals("v1", result.engineVersion());
        assertFalse(result.isEmpty());
    }

    @Test
    void scoreResult_empty_deberiaSerVacio() {
        var empty = ScoreResult.empty();
        assertTrue(empty.isEmpty());
        assertEquals(0.0, empty.confidence());
        assertEquals("NO_EVALUATED", empty.viabilityLabel());
        assertEquals("ScoringEngine", empty.generatedBy());
    }

    private ProjectContext richContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.PROBLEM, "p".repeat(120));
        data.put(AnalyzedDimension.SOLUTION, "s".repeat(120));
        data.put(AnalyzedDimension.TARGET_CUSTOMER, "c".repeat(120));
        data.put(AnalyzedDimension.REVENUE_MODEL, "r".repeat(120));
        data.put(AnalyzedDimension.VALUE_PROPOSITION, "v".repeat(120));
        data.put(AnalyzedDimension.COMPETITION, "cp".repeat(60));
        data.put(AnalyzedDimension.MVP, "m".repeat(60));
        data.put(AnalyzedDimension.RISKS, "ri".repeat(60));
        data.put(AnalyzedDimension.RESOURCES, "re".repeat(60));
        data.put(AnalyzedDimension.SCALABILITY, "sc".repeat(60));
        data.put(AnalyzedDimension.PROJECT_NAME, "Mi App");
        data.put(AnalyzedDimension.SECTOR, "Software");
        return ProjectContext.restore(
            new EnumMap<>(data), java.util.EnumSet.copyOf(data.keySet()),
            null, 5, false);
    }

    private CompletenessEvaluation richEvaluation() {
        return new CompletenessEvaluation(
            0.85, List.of(), List.of(), 0.9,
            CompletenessEvaluation.MaturityLevel.MATURE,
            CompletenessEvaluation.ViabilityLevel.VERY_HIGH, 0.8,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 12, AnalyzedDimension.values().length);
    }
}
