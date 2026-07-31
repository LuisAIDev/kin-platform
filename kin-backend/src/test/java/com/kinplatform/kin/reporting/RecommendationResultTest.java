package com.kinplatform.kin.reporting;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationResultTest {

    private final Recommendation rec = Recommendation.create(
        RecommendationCategory.STRATEGY, "Título", "Descripción",
        7, ImpactLevel.HIGH, EffortLevel.MEDIUM, AnalyzedDimension.PROBLEM,
        List.of("Paso 1", "Paso 2"), "Resultado esperado",
        RecommendationExplanation.of(List.of("Dato A"), "Regla X", "Motivo Y"));

    @Test
    void resultado_deberiaAceptarListaNula() {
        var result = new RecommendationResult(null, 5, 0.5,
            RecommendationCategory.STRATEGY, "x", "e", "v1");
        assertTrue(result.recommendations().isEmpty());
        assertEquals(5, result.priority());
    }

    @Test
    void resultado_deberiaSerInmutable() {
        var recs = new ArrayList<>(List.of(rec));
        var result = new RecommendationResult(recs, 7, 0.8,
            RecommendationCategory.STRATEGY, "explicación", "engine", "v1");

        recs.clear();
        assertEquals(1, result.recommendations().size());
        assertThrows(UnsupportedOperationException.class,
            () -> result.recommendations().add(rec));
    }

    @Test
    void resultado_deberiaAcotarPrioridadYConfianza() {
        var result = new RecommendationResult(List.of(), 99, 2.0,
            RecommendationCategory.VALIDATION, "x", "e", "v1");
        assertEquals(10, result.priority());
        assertEquals(1.0, result.confidence());

        var result2 = new RecommendationResult(List.of(), -5, -1.0,
            RecommendationCategory.VALIDATION, "x", "e", "v1");
        assertEquals(0, result2.priority());
        assertEquals(0.0, result2.confidence());
    }

    @Test
    void vacio_deberiaSerSinRecomendaciones() {
        var result = RecommendationResult.empty();
        assertFalse(result.hasRecommendations());
        assertEquals(0, result.priority());
        assertEquals(0.0, result.confidence());
    }

    @Test
    void recomendacion_deberiaProtegerListaDePasos() {
        var steps = new ArrayList<>(List.of("A"));
        var r = new Recommendation(null, RecommendationCategory.MARKETING, "t", "d",
            5, ImpactLevel.LOW, EffortLevel.LOW, null, steps, "out",
            RecommendationExplanation.of(List.of(), "r", "y"));
        steps.clear();
        assertEquals(1, r.actionableSteps().size());
    }

    @Test
    void recomendacion_deberiaAcotarPrioridad() {
        var r = new Recommendation(null, RecommendationCategory.MARKETING, "t", "d",
            99, ImpactLevel.LOW, EffortLevel.LOW, null, List.of(), "out",
            RecommendationExplanation.of(List.of(), "r", "y"));
        assertEquals(10, r.priority());
    }

    @Test
    void create_deberiaGenerarIdDeterminista() {
        var r1 = Recommendation.create(RecommendationCategory.FINANCIAL, "T", "D",
            8, ImpactLevel.CRITICAL, EffortLevel.HIGH, AnalyzedDimension.REVENUE_MODEL,
            List.of("P"), "O", RecommendationExplanation.of(List.of(), "R", "Y"));
        var r2 = Recommendation.create(RecommendationCategory.FINANCIAL, "T", "D",
            8, ImpactLevel.CRITICAL, EffortLevel.HIGH, AnalyzedDimension.REVENUE_MODEL,
            List.of("P"), "O", RecommendationExplanation.of(List.of(), "R", "Y"));
        assertEquals(r1.id(), r2.id());
    }

    @Test
    void explicacion_deberiaProtegerListaDeInformacion() {
        var info = new ArrayList<>(List.of("dato"));
        var exp = new RecommendationExplanation(info, "r", "y");
        info.clear();
        assertEquals(1, exp.usedInformation().size());
    }

    @Test
    void explicacion_deberiaAceptarListaNula() {
        var exp = new RecommendationExplanation(null, "r", "y");
        assertTrue(exp.usedInformation().isEmpty());
    }

    @Test
    void modelo_deberiaTenerValoresPorDefecto() {
        var model = RecommendationModel.defaultModel();
        assertEquals(40, model.lowScoreThreshold());
        assertEquals(70, model.highScoreThreshold());
        assertEquals(0.6, model.minCoverageForMature());
        assertEquals("v1", model.version());
    }

    @Test
    void categoria_deberiaTenerNombreAmigable() {
        assertEquals("Validación", RecommendationCategory.VALIDATION.displayName());
        assertEquals("Financiero", RecommendationCategory.FINANCIAL.displayName());
    }

    @Test
    void recomendacion_deberiaAceptarExplicacionNulaComoVacia() {
        var r = Recommendation.create(RecommendationCategory.PRODUCT, "t", "d",
            5, ImpactLevel.LOW, EffortLevel.LOW, null, null, "o", null);
        assertNotNull(r.explanation());
        assertTrue(r.explanation().usedInformation().isEmpty());
    }

    @Test
    void input_deberiaSerInmutablePorDefecto() {
        var input = new RecommendationInput(null, null, null, null);
        assertNotNull(input);
        assertNull(input.projectContext());
    }
}
