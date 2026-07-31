package com.kinplatform.kin.reporting;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {

    private final RecommendationEngine engine =
        new RecommendationEngine(RecommendationModel.defaultModel());

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ProjectContext projectWith(Map<AnalyzedDimension, String> dims) {
        var ctx = ProjectContext.fromProject("Proyecto Test", "Solución innovadora", "Tecnología");
        var extra = new LinkedHashMap<AnalyzedDimension, String>(dims);
        extra.remove(AnalyzedDimension.PROJECT_NAME);
        extra.remove(AnalyzedDimension.SECTOR);
        ctx.update(new AnalysisResult(extra));
        return ctx;
    }

    private CompletenessEvaluation evaluation(double coverage,
                                              List<AnalyzedDimension> missing,
                                              List<AnalyzedDimension> criticalMissing,
                                              CompletenessEvaluation.MaturityLevel maturity,
                                              double quality,
                                              int covered) {
        return new CompletenessEvaluation(
            coverage, missing, criticalMissing, 0.8,
            maturity, CompletenessEvaluation.ViabilityLevel.MEDIUM, quality,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, covered, AnalyzedDimension.values().length
        );
    }

    private ScoreResult score(int total, Map<String, Integer> categoryScores) {
        return new ScoreResult(total, 100, categoryScores, "MEDIA", List.of(), List.of(), "");
    }

    private ScoreResult emptyScore() {
        return ScoreResult.empty();
    }

    private RecommendationInput input(ProjectContext ctx, CompletenessEvaluation eval,
                                      ScoreResult score) {
        return new RecommendationInput(ctx, eval,
            ConversationDecision.generateReport("generar reporte"), score);
    }

    // ---------------------------------------------------------------
    // Test 1 — entrada nula devuelve resultado vacío
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaRetornarVacio_cuandoEntradaNula() {
        var result = engine.evaluate(null);
        assertNotNull(result);
        assertFalse(result.hasRecommendations());
        assertEquals(0, result.priority());
        assertEquals(0.0, result.confidence());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoFaltanDatos() {
        var result = engine.evaluate(new RecommendationInput(null, null, null, null));
        assertFalse(result.hasRecommendations());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoProyectoEsNulo() {
        var eval = evaluation(0.5, List.of(), List.of(),
            CompletenessEvaluation.MaturityLevel.DEVELOPING, 0.6, 7);
        var result = engine.evaluate(new RecommendationInput(null, eval,
            ConversationDecision.generateReport("r"), score(50, Map.of())));
        assertFalse(result.hasRecommendations());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoEvaluacionEsNula() {
        var ctx = projectWith(Map.of());
        var result = engine.evaluate(new RecommendationInput(ctx, null,
            ConversationDecision.generateReport("r"), score(50, Map.of())));
        assertFalse(result.hasRecommendations());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoScoreEsNulo() {
        var ctx = projectWith(Map.of());
        var eval = evaluation(0.5, List.of(), List.of(),
            CompletenessEvaluation.MaturityLevel.DEVELOPING, 0.6, 7);
        var result = engine.evaluate(new RecommendationInput(ctx, eval,
            ConversationDecision.generateReport("r"), null));
        assertFalse(result.hasRecommendations());
    }

    // ---------------------------------------------------------------
    // Test 2 — proyecto inmaduro genera muchas recomendaciones
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaGenerarRecomendaciones_cuandoProyectoInmaduro() {
        var ctx = projectWith(Map.of(AnalyzedDimension.PROBLEM, "Resolver la logística urbana"));
        var eval = evaluation(0.2,
            List.of(AnalyzedDimension.TARGET_CUSTOMER, AnalyzedDimension.REVENUE_MODEL,
                AnalyzedDimension.VALUE_PROPOSITION, AnalyzedDimension.MVP),
            List.of(AnalyzedDimension.TARGET_CUSTOMER, AnalyzedDimension.REVENUE_MODEL),
            CompletenessEvaluation.MaturityLevel.EARLY, 0.3, 4);
        var result = engine.evaluate(input(ctx, eval, score(20, Map.of())));

        assertTrue(result.hasRecommendations());
        assertNotNull(result.generatedBy());
        assertEquals(RecommendationEngine.GENERATOR_NAME, result.generatedBy());
        assertNotNull(result.engineVersion());
        assertEquals("v1", result.engineVersion());

        // 11 dimensiones faltantes del contexto + 1 validación temprana + 1 recolección general
        assertEquals(13, result.recommendations().size());

        // Las dimensiones críticas faltantes tienen prioridad 9
        var critical = result.recommendations().stream()
            .filter(r -> r.priority() == 9)
            .toList();
        assertEquals(2, critical.size());

        // La recomendación de validación temprana está presente
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.category() == RecommendationCategory.VALIDATION));

        // La prioridad global es el máximo
        assertEquals(9, result.priority());

        // Cada recomendación tiene explicación auditable
        for (var r : result.recommendations()) {
            assertNotNull(r.explanation());
            assertFalse(r.explanation().usedInformation().isEmpty());
            assertFalse(r.explanation().appliedRule().isBlank());
            assertFalse(r.explanation().reason().isBlank());
        }
    }

    // ---------------------------------------------------------------
    // Test 3 — proyecto maduro completo y con score alto: sin recomendaciones
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaRetornarSinRecomendaciones_cuandoProyectoMaduroCompleto() {
        var allDims = new LinkedHashMap<AnalyzedDimension, String>();
        for (var dim : AnalyzedDimension.values()) {
            allDims.put(dim, "Información detallada y completa de la dimensión con suficientes detalles.");
        }
        var ctx = projectWith(allDims);
        var eval = evaluation(1.0, List.of(), List.of(),
            CompletenessEvaluation.MaturityLevel.MATURE, 0.95, AnalyzedDimension.values().length);
        var result = engine.evaluate(input(ctx, eval, score(90, Map.of())));

        assertFalse(result.hasRecommendations());
        assertTrue(result.recommendations().isEmpty());
        assertEquals(0, result.priority());
    }

    // ---------------------------------------------------------------
    // Test 4 — score alto con brechas pendientes: recomendación de innovación
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaGenerarInnovacion_cuandoScoreAltoYPendencias() {
        var dims = new LinkedHashMap<AnalyzedDimension, String>();
        dims.put(AnalyzedDimension.PROBLEM, "Problema bien definido con contexto claro");
        dims.put(AnalyzedDimension.SOLUTION, "Solución diferenciada con detalle técnico");
        dims.put(AnalyzedDimension.TARGET_CUSTOMER, "Segmento joven urbano digital");
        dims.put(AnalyzedDimension.VALUE_PROPOSITION, "Valor claro y comunicable");
        dims.put(AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción mensual");
        dims.put(AnalyzedDimension.COMPETITION, "Dos competidores directos");
        dims.put(AnalyzedDimension.RISKS, "Riesgos identificados y mitigados");
        dims.put(AnalyzedDimension.RESOURCES, "Equipo y recursos definidos");
        dims.put(AnalyzedDimension.MVP, "MVP con métricas de validación");
        dims.put(AnalyzedDimension.OBJECTIVES, "Objetivos SMART definidos");
        dims.put(AnalyzedDimension.SECTOR, "Sector tecnología creciente");
        dims.put(AnalyzedDimension.CITY, "Ciudad de México");
        var ctx = projectWith(dims);
        var eval = evaluation(0.92,
            List.of(AnalyzedDimension.SCALABILITY),
            List.of(),
            CompletenessEvaluation.MaturityLevel.MATURE, 0.9, 13);
        var result = engine.evaluate(input(ctx, eval, score(85, Map.of())));

        assertTrue(result.hasRecommendations());
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.category() == RecommendationCategory.INNOVATION));
        // Cobertura de la dimensión faltante
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.relatedDimension() == AnalyzedDimension.SCALABILITY));
    }

    // ---------------------------------------------------------------
    // Test 5 — score bajo refuerza el pilar más débil
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaReforzarPilarDebil_cuandoScoreBajo() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema claramente definido",
            AnalyzedDimension.SOLUTION, "Solución con detalles",
            AnalyzedDimension.TARGET_CUSTOMER, "Clientes definidos",
            AnalyzedDimension.REVENUE_MODEL, "Modelo de ingresos",
            AnalyzedDimension.COMPETITION, "Competencia mapeada"));
        var eval = evaluation(0.5,
            List.of(AnalyzedDimension.RISKS, AnalyzedDimension.MVP, AnalyzedDimension.SCALABILITY),
            List.of(),
            CompletenessEvaluation.MaturityLevel.DEVELOPING, 0.5, 5);

        var categoryScores = new LinkedHashMap<String, Integer>();
        categoryScores.put(AnalyzedDimension.PROBLEM.displayName(), 3);
        categoryScores.put(AnalyzedDimension.REVENUE_MODEL.displayName(), 2);
        var result = engine.evaluate(input(ctx, eval, score(25, categoryScores)));

        assertTrue(result.hasRecommendations());
        var financial = result.recommendations().stream()
            .filter(r -> r.category() == RecommendationCategory.FINANCIAL
                && r.priority() == 8)
            .findFirst();
        assertTrue(financial.isPresent());
        // El pilar más débil con puntaje > 0 es REVENUE_MODEL (2)
        assertEquals(AnalyzedDimension.REVENUE_MODEL, financial.get().relatedDimension());
    }

    // ---------------------------------------------------------------
    // Test 6 — información insuficiente: recomendación general de recolección
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaSugerirRecolectarInformacion_cuandoInformacionInsuficiente() {
        var ctx = projectWith(Map.of());
        var eval = evaluation(0.1, List.of(), List.of(),
            CompletenessEvaluation.MaturityLevel.EARLY, 0.1, 3);
        var result = engine.evaluate(input(ctx, eval, emptyScore()));

        assertTrue(result.hasRecommendations());
        // Sin dimensiones con puntaje positivo: recomendación general de validación
        assertTrue(result.recommendations().stream()
            .anyMatch(r -> r.category() == RecommendationCategory.VALIDATION
                && r.relatedDimension() == null
                && r.priority() == 8));
    }

    // ---------------------------------------------------------------
    // Test 7 — determinismo: mismas entradas producen mismas recomendaciones
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaSerDeterminista() {
        var ctx = projectWith(Map.of(AnalyzedDimension.PROBLEM, "Problema definido"));
        var eval = evaluation(0.3,
            List.of(AnalyzedDimension.TARGET_CUSTOMER, AnalyzedDimension.MVP),
            List.of(AnalyzedDimension.TARGET_CUSTOMER),
            CompletenessEvaluation.MaturityLevel.EARLY, 0.3, 4);
        var input1 = input(ctx, eval, score(30, Map.of()));
        var input2 = input(ctx, eval, score(30, Map.of()));

        var r1 = engine.evaluate(input1);
        var r2 = engine.evaluate(input2);

        assertEquals(r1.recommendations().size(), r2.recommendations().size());
        for (int i = 0; i < r1.recommendations().size(); i++) {
            assertEquals(r1.recommendations().get(i).id(), r2.recommendations().get(i).id());
            assertEquals(r1.recommendations().get(i).title(), r2.recommendations().get(i).title());
            assertEquals(r1.recommendations().get(i).priority(), r2.recommendations().get(i).priority());
        }
    }

    // ---------------------------------------------------------------
    // Test 8 — la prioridad de las recomendaciones está ordenada desc
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaOrdenarRecomendacionesPorPrioridadDescendente() {
        var ctx = projectWith(Map.of());
        var eval = evaluation(0.2,
            List.of(AnalyzedDimension.REVENUE_MODEL, AnalyzedDimension.MVP),
            List.of(AnalyzedDimension.REVENUE_MODEL),
            CompletenessEvaluation.MaturityLevel.EARLY, 0.3, 3);
        var result = engine.evaluate(input(ctx, eval, score(30, Map.of())));

        for (int i = 1; i < result.recommendations().size(); i++) {
            assertTrue(result.recommendations().get(i - 1).priority()
                >= result.recommendations().get(i).priority());
        }
    }

    // ---------------------------------------------------------------
    // Test 9 — categoría dominante es la más frecuente
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaCalcularCategoriaDominante() {
        var ctx = projectWith(Map.of());
        var eval = evaluation(0.2,
            List.of(AnalyzedDimension.REVENUE_MODEL, AnalyzedDimension.RISKS,
                AnalyzedDimension.MVP, AnalyzedDimension.SCALABILITY),
            List.of(AnalyzedDimension.REVENUE_MODEL, AnalyzedDimension.RISKS),
            CompletenessEvaluation.MaturityLevel.EARLY, 0.3, 3);
        var result = engine.evaluate(input(ctx, eval, score(30, Map.of())));

        var counts = result.recommendations().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                Recommendation::category, java.util.stream.Collectors.counting()));
        var dominantCount = counts.getOrDefault(result.category(), 0L);
        var max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        assertEquals(max, dominantCount);
    }

    // ---------------------------------------------------------------
    // Test 10 — la confianza es determinista y está en [0, 1]
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaCalcularConfianzaDeterminista() {
        var ctx = projectWith(Map.of(AnalyzedDimension.PROBLEM, "Problema bien definido"));
        var eval = evaluation(0.6,
            List.of(AnalyzedDimension.TARGET_CUSTOMER),
            List.of(AnalyzedDimension.TARGET_CUSTOMER),
            CompletenessEvaluation.MaturityLevel.DEVELOPING, 0.7, 4);
        var r1 = engine.evaluate(input(ctx, eval, score(60, Map.of())));
        var r2 = engine.evaluate(input(ctx, eval, score(60, Map.of())));

        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertTrue(r1.confidence() >= 0.0 && r1.confidence() <= 1.0);
    }
}
