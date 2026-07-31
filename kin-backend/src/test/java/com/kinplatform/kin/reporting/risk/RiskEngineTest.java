package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineTest {

    private RiskEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskEngine(
            List.of(
                new BusinessRiskAnalyzer(),
                new TechnicalRiskAnalyzer(),
                new FinancialRiskAnalyzer(),
                new MarketRiskAnalyzer()),
            RiskModel.defaultModel());
    }

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

    private CompletenessEvaluation evaluation(double coverage, int covered) {
        return new CompletenessEvaluation(
            coverage, List.of(), List.of(), 0.8,
            CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, 0.7,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, covered, AnalyzedDimension.values().length);
    }

    private ScoreResult score(int total) {
        return new ScoreResult(total, 100, Map.of(), "MEDIA", List.of(), List.of(), "");
    }

    private RiskInput input(ProjectContext ctx, CompletenessEvaluation eval, ScoreResult s) {
        return new RiskInput(ctx, eval, ConversationDecision.generateReport("reporte"), s);
    }

    // ---------------------------------------------------------------
    // Test 1 — proyecto sin riesgos (todas las dimensiones cubiertas)
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaRetornarSinRiesgos_cuandoProyectoCompleto() {
        var allDims = new LinkedHashMap<AnalyzedDimension, String>();
        for (var dim : AnalyzedDimension.values()) {
            allDims.put(dim, "Información detallada y completa de la dimensión con suficientes detalles.");
        }
        var ctx = projectWith(allDims);
        var result = engine.evaluate(input(ctx, evaluation(1.0, AnalyzedDimension.values().length),
            score(85)));

        assertNotNull(result);
        assertFalse(result.hasRisks());
        assertEquals(0, result.riskCount());
        assertEquals(RiskLevel.LOW, result.overallRiskLevel());
        assertEquals(RiskLevel.LOW, result.highestRiskLevel());
        assertEquals(RiskEngine.GENERATOR_NAME, result.generatedBy());
    }

    // ---------------------------------------------------------------
    // Test 2 — riesgo financiero
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarRiesgoFinanciero() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.TARGET_CUSTOMER, "Segmento definido",
            AnalyzedDimension.VALUE_PROPOSITION, "Valor claro",
            AnalyzedDimension.COMPETITION, "Competencia mapeada",
            AnalyzedDimension.MVP, "MVP definido",
            AnalyzedDimension.SCALABILITY, "Escalabilidad evaluada",
            AnalyzedDimension.OBJECTIVES, "Objetivos SMART"));
        var result = engine.evaluate(input(ctx, evaluation(0.8, 12), score(70)));

        assertTrue(result.hasRisks());
        assertTrue(result.risks().stream()
            .anyMatch(r -> r.category() == RiskCategory.FINANCIAL
                && r.relatedDimension() == AnalyzedDimension.REVENUE_MODEL
                && r.severity() == RiskLevel.CRITICAL));
        // Los riesgos deben ser explicables y con evidencia
        for (var r : result.risks()) {
            assertNotNull(r.explanation());
            assertFalse(r.explanation().usedInformation().isEmpty());
            assertFalse(r.explanation().appliedRule().isBlank());
            assertFalse(r.explanation().reason().isBlank());
            assertFalse(r.explanation().evidence().isBlank());
            assertFalse(r.appliedRules().isEmpty());
            assertFalse(r.engineVersion().isBlank());
        }
    }

    // ---------------------------------------------------------------
    // Test 3 — riesgo técnico
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarRiesgoTecnico() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.TARGET_CUSTOMER, "Segmento definido",
            AnalyzedDimension.VALUE_PROPOSITION, "Valor claro",
            AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción",
            AnalyzedDimension.COMPETITION, "Competencia mapeada",
            AnalyzedDimension.RESOURCES, "Recursos definidos",
            AnalyzedDimension.OBJECTIVES, "Objetivos SMART",
            AnalyzedDimension.SECTOR, "Sector tecnología"));
        var result = engine.evaluate(input(ctx, evaluation(0.8, 12), score(75)));

        assertTrue(result.hasRisks());
        assertTrue(result.risks().stream()
            .anyMatch(r -> r.category() == RiskCategory.TECHNICAL
                && r.relatedDimension() == AnalyzedDimension.MVP));
    }

    // ---------------------------------------------------------------
    // Test 4 — riesgo de mercado
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarRiesgoDeMercado() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción",
            AnalyzedDimension.VALUE_PROPOSITION, "Valor claro",
            AnalyzedDimension.MVP, "MVP definido",
            AnalyzedDimension.RESOURCES, "Recursos definidos",
            AnalyzedDimension.SCALABILITY, "Escalabilidad evaluada",
            AnalyzedDimension.OBJECTIVES, "Objetivos SMART"));
        var result = engine.evaluate(input(ctx, evaluation(0.8, 12), score(80)));

        assertTrue(result.hasRisks());
        assertTrue(result.risks().stream()
            .anyMatch(r -> r.category() == RiskCategory.MARKET
                && r.relatedDimension() == AnalyzedDimension.TARGET_CUSTOMER));
        assertTrue(result.risks().stream()
            .anyMatch(r -> r.category() == RiskCategory.MARKET
                && r.relatedDimension() == AnalyzedDimension.COMPETITION));
    }

    @Test
    void evaluate_deberiaIdentificarRiesgoDeSector() {
        var ctx = new ProjectContext();
        ctx.update(new AnalysisResult(new LinkedHashMap<>(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción"))));
        var result = engine.evaluate(input(ctx, evaluation(0.4, 5), score(60)));

        assertTrue(result.risks().stream()
            .anyMatch(r -> r.category() == RiskCategory.MARKET
                && r.relatedDimension() == AnalyzedDimension.SECTOR));
    }

    // ---------------------------------------------------------------
    // Test 5 — múltiples riesgos (proyecto inmaduro)
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarMultiplesRiesgos_cuandoProyectoInmaduro() {
        var ctx = projectWith(Map.of(AnalyzedDimension.PROBLEM, "Problema definido"));
        var result = engine.evaluate(input(ctx, evaluation(0.3, 4), score(25)));

        assertTrue(result.hasRisks());
        assertTrue(result.riskCount() >= 4);
        // Debe haber al menos un riesgo por categoría
        for (var category : RiskCategory.values()) {
            assertTrue(result.risks().stream().anyMatch(r -> r.category() == category),
                "Debe haber un riesgo de categoría " + category);
        }
        // Nivel global = máxima severidad
        assertEquals(RiskLevel.CRITICAL, result.overallRiskLevel());
        assertEquals(result.overallRiskLevel(), result.highestRiskLevel());
        // topRisks no vacío y ordenado por severityScore desc
        assertFalse(result.topRisks().isEmpty());
        for (int i = 1; i < result.topRisks().size(); i++) {
            assertTrue(result.topRisks().get(i - 1).severityScore() >= result.topRisks().get(i).severityScore());
        }
    }

    // ---------------------------------------------------------------
    // Test 6 — datos insuficientes
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaRetornarVacio_cuandoEntradaNula() {
        var result = engine.evaluate(null);
        assertFalse(result.hasRisks());
        assertEquals(0, result.riskCount());
        assertEquals(RiskLevel.LOW, result.overallRiskLevel());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoFaltanDatos() {
        var result = engine.evaluate(new RiskInput(null, null, null, null));
        assertFalse(result.hasRisks());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoEvaluacionEsNula() {
        var ctx = projectWith(Map.of());
        var result = engine.evaluate(new RiskInput(ctx, null,
            ConversationDecision.generateReport("r"), score(50)));
        assertFalse(result.hasRisks());
    }

    // ---------------------------------------------------------------
    // Test 7 — determinismo
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaSerDeterminista() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción",
            AnalyzedDimension.COMPETITION, "Competencia mapeada"));
        var in = input(ctx, evaluation(0.5, 6), score(55));

        var r1 = engine.evaluate(in);
        var r2 = engine.evaluate(in);

        assertEquals(r1.riskCount(), r2.riskCount());
        for (int i = 0; i < r1.risks().size(); i++) {
            assertEquals(r1.risks().get(i).id(), r2.risks().get(i).id());
            assertEquals(r1.risks().get(i).title(), r2.risks().get(i).title());
            assertEquals(r1.risks().get(i).severity(), r2.risks().get(i).severity());
            assertEquals(r1.risks().get(i).confidence(), r2.risks().get(i).confidence(), 1e-9);
        }
        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.explanation(), r2.explanation());
    }

    // ---------------------------------------------------------------
    // Test 8 — auto-descubrimiento de analizadores
    // ---------------------------------------------------------------
    @Test
    void engine_deberiaDescubrirAnalizadoresRegistrados() {
        var engine2 = new RiskEngine(List.of(new BusinessRiskAnalyzer()), RiskModel.defaultModel());
        assertEquals(1, engine2.analyzers().size());
        assertEquals(RiskCategory.BUSINESS, engine2.analyzers().get(0).category());

        // Solo analiza riesgos de la categoría registrada
        var ctx = projectWith(Map.of());
        var result = engine2.evaluate(input(ctx, evaluation(0.2, 3), score(30)));
        assertTrue(result.hasRisks());
        assertTrue(result.risks().stream().allMatch(r -> r.category() == RiskCategory.BUSINESS));
    }
}
