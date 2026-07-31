package com.kinplatform.kin.reporting.opportunity;

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

class OpportunityEngineTest {

    private OpportunityEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OpportunityEngine(
            List.of(
                new MarketOpportunityAnalyzer(),
                new InnovationOpportunityAnalyzer(),
                new TechnologicalOpportunityAnalyzer(),
                new FinancialOpportunityAnalyzer(),
                new CompetitiveOpportunityAnalyzer(),
                new ScalabilityOpportunityAnalyzer(),
                new AutomationOpportunityAnalyzer(),
                new MonetizationOpportunityAnalyzer()),
            OpportunityModel.defaultModel());
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

    private CompletenessEvaluation evaluation(double coverage, int covered,
                                              List<AnalyzedDimension> criticalMissing) {
        return new CompletenessEvaluation(
            coverage, List.of(), criticalMissing, 0.8,
            CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, 0.7,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, covered, AnalyzedDimension.values().length);
    }

    private ScoreResult score(int total) {
        return new ScoreResult(total, 100, Map.of(), "MEDIA", List.of(), List.of(), "");
    }

    private OpportunityInput input(ProjectContext ctx, CompletenessEvaluation eval, ScoreResult s) {
        return new OpportunityInput(ctx, eval, ConversationDecision.generateReport("reporte"), s);
    }

    // ---------------------------------------------------------------
    // Test 1 — proyecto completo sin oportunidades
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaRetornarSinOportunidades_cuandoProyectoCompleto() {
        var allDims = new LinkedHashMap<AnalyzedDimension, String>();
        for (var dim : AnalyzedDimension.values()) {
            allDims.put(dim, "Información detallada y completa de la dimensión con suficientes detalles.");
        }
        var ctx = projectWith(allDims);
        var result = engine.evaluate(input(ctx, evaluation(1.0, AnalyzedDimension.values().length,
            List.of()), score(85)));

        assertNotNull(result);
        assertFalse(result.hasOpportunities());
        assertEquals(0, result.opportunityCount());
        assertEquals(0, result.highestPriority());
        assertEquals(OpportunityEngine.GENERATOR_NAME, result.generatedBy());
    }

    // ---------------------------------------------------------------
    // Test 2 — oportunidad de monetización (dimensión crítica ausente)
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarOportunidadDeMonetizacion() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.TARGET_CUSTOMER, "Segmento definido",
            AnalyzedDimension.VALUE_PROPOSITION, "Valor claro",
            AnalyzedDimension.COMPETITION, "Competencia mapeada",
            AnalyzedDimension.MVP, "MVP definido",
            AnalyzedDimension.SCALABILITY, "Escalabilidad evaluada",
            AnalyzedDimension.OBJECTIVES, "Objetivos SMART"));
        var result = engine.evaluate(input(ctx, evaluation(0.8, 12, List.of()), score(70)));

        assertTrue(result.hasOpportunities());
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.MONETIZACION
                && o.relatedDimension() == AnalyzedDimension.REVENUE_MODEL
                && o.impactLevel() == com.kinplatform.kin.reporting.ImpactLevel.CRITICAL));
        // Las oportunidades deben ser explicables y con evidencia
        for (var o : result.opportunities()) {
            assertNotNull(o.explanation());
            assertFalse(o.explanation().usedInformation().isEmpty());
            assertFalse(o.explanation().appliedRule().isBlank());
            assertFalse(o.explanation().reason().isBlank());
            assertFalse(o.explanation().evidence().isBlank());
            assertFalse(o.appliedRules().isEmpty());
            assertFalse(o.engineVersion().isBlank());
            assertTrue(o.priority() >= 1 && o.priority() <= 10);
        }
    }

    // ---------------------------------------------------------------
    // Test 3 — oportunidad de escalabilidad
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarOportunidadDeEscalabilidad() {
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
        var result = engine.evaluate(input(ctx, evaluation(0.8, 12, List.of()), score(75)));

        assertTrue(result.hasOpportunities());
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.ESCALABILIDAD
                && o.relatedDimension() == AnalyzedDimension.SCALABILITY));
    }

    // ---------------------------------------------------------------
    // Test 4 — oportunidades de mercado (cliente objetivo)
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarOportunidadDeMercado() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción",
            AnalyzedDimension.VALUE_PROPOSITION, "Valor claro",
            AnalyzedDimension.MVP, "MVP definido",
            AnalyzedDimension.RESOURCES, "Recursos definidos",
            AnalyzedDimension.SCALABILITY, "Escalabilidad evaluada",
            AnalyzedDimension.OBJECTIVES, "Objetivos SMART"));
        var result = engine.evaluate(input(ctx, evaluation(0.8, 12, List.of()), score(80)));

        assertTrue(result.hasOpportunities());
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.MERCADO
                && o.relatedDimension() == AnalyzedDimension.TARGET_CUSTOMER));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.COMPETITIVA
                && o.relatedDimension() == AnalyzedDimension.COMPETITION));
    }

    // ---------------------------------------------------------------
    // Test 5 — múltiples oportunidades (proyecto inmaduro)
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarMultiplesOportunidades_cuandoProyectoInmaduro() {
        var ctx = projectWith(Map.of(AnalyzedDimension.PROBLEM, "Problema definido"));
        var result = engine.evaluate(input(ctx, evaluation(0.3, 4, List.of()), score(25)));

        assertTrue(result.hasOpportunities());
        assertTrue(result.opportunityCount() >= 5);
        // Debe haber al menos una oportunidad de varias categorías
        for (var category : List.of(OpportunityCategory.MERCADO, OpportunityCategory.INNOVACION,
            OpportunityCategory.FINANCIERA, OpportunityCategory.MONETIZACION)) {
            assertTrue(result.opportunities().stream().anyMatch(o -> o.category() == category),
                "Debe haber una oportunidad de categoría " + category);
        }
        // topOpportunities no vacío y ordenado por prioridad desc
        assertFalse(result.topOpportunities().isEmpty());
        for (int i = 1; i < result.topOpportunities().size(); i++) {
            assertTrue(result.topOpportunities().get(i - 1).priority()
                >= result.topOpportunities().get(i).priority());
        }
    }

    // ---------------------------------------------------------------
    // Test 6 — prioridad alta cuando la dimensión es crítica
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaAumentarPrioridad_cuandoDimensionCriticaAusente() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.PROBLEM, "Problema definido claramente",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico",
            AnalyzedDimension.TARGET_CUSTOMER, "Segmento definido",
            AnalyzedDimension.VALUE_PROPOSITION, "Valor claro",
            AnalyzedDimension.COMPETITION, "Competencia mapeada",
            AnalyzedDimension.MVP, "MVP definido",
            AnalyzedDimension.SCALABILITY, "Escalabilidad evaluada",
            AnalyzedDimension.OBJECTIVES, "Objetivos SMART"));
        // REVENUE_MODEL como crítica ausente
        var eval = new CompletenessEvaluation(
            0.8, List.of(), List.of(AnalyzedDimension.REVENUE_MODEL), 0.8,
            CompletenessEvaluation.MaturityLevel.DEVELOPING,
            CompletenessEvaluation.ViabilityLevel.MEDIUM, 0.7,
            List.of(), List.of(), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, 12, AnalyzedDimension.values().length);
        var result = engine.evaluate(input(ctx, eval, score(70)));

        var monetization = result.opportunities().stream()
            .filter(o -> o.category() == OpportunityCategory.MONETIZACION
                && o.relatedDimension() == AnalyzedDimension.REVENUE_MODEL)
            .findFirst()
            .orElseThrow();
        // Sin señal: prioridad = 2 + missingBonus(3) = 5 (score 70 → base 2)
        assertEquals(5, monetization.priority());
    }

    // ---------------------------------------------------------------
    // Test 7 — señal detectada aumenta prioridad
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaExplotarSenalDetectada() {
        var allDims = new LinkedHashMap<AnalyzedDimension, String>();
        for (var dim : AnalyzedDimension.values()) {
            allDims.put(dim, "Información detallada y completa.");
        }
        var ctx = projectWith(allDims);
        var eval = new CompletenessEvaluation(
            1.0, List.of(), List.of(), 1.0,
            CompletenessEvaluation.MaturityLevel.MATURE,
            CompletenessEvaluation.ViabilityLevel.HIGH, 1.0,
            List.of(), List.of("monetización vía suscripción"), List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, AnalyzedDimension.values().length, AnalyzedDimension.values().length);
        var result = engine.evaluate(input(ctx, eval, score(85)));

        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.MONETIZACION
                && o.priority() >= 1));
        assertTrue(result.explanation().toLowerCase().contains("monetización"));
    }

    // ---------------------------------------------------------------
    // Test 7b — señales de mercado / tecnología / escalabilidad
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaExplotarSeñalesDeMercadoTecnologiaYEscalabilidad() {
        var allDims = new LinkedHashMap<AnalyzedDimension, String>();
        for (var dim : AnalyzedDimension.values()) {
            allDims.put(dim, "Información detallada y completa.");
        }
        var ctx = projectWith(allDims);
        var eval = new CompletenessEvaluation(
            1.0, List.of(), List.of(), 1.0,
            CompletenessEvaluation.MaturityLevel.MATURE,
            CompletenessEvaluation.ViabilityLevel.HIGH, 1.0,
            List.of(), List.of("mercado internacional", "tecnología propia", "escalar a otras regiones"),
            List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, AnalyzedDimension.values().length, AnalyzedDimension.values().length);
        var result = engine.evaluate(input(ctx, eval, score(90)));

        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.MERCADO
                && o.priority() >= 1));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.TECNOLOGICA
                && o.priority() >= 1));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.ESCALABILIDAD
                && o.priority() >= 1));
    }

    // ---------------------------------------------------------------
    // Test 7d — señales de automatización / competencia / financiera / innovación
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaExplotarSeñalesDeAutomatizacionCompetenciaFinancieraEInnovacion() {
        var allDims = new LinkedHashMap<AnalyzedDimension, String>();
        for (var dim : AnalyzedDimension.values()) {
            allDims.put(dim, "Información detallada y completa.");
        }
        var ctx = projectWith(allDims);
        var eval = new CompletenessEvaluation(
            1.0, List.of(), List.of(), 1.0,
            CompletenessEvaluation.MaturityLevel.MATURE,
            CompletenessEvaluation.ViabilityLevel.HIGH, 1.0,
            List.of(), List.of("automatizar operaciones", "ventaja competitiva",
                "oportunidad financiera", "innovación de producto"),
            List.of(),
            CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT,
            true, AnalyzedDimension.values().length, AnalyzedDimension.values().length);
        var result = engine.evaluate(input(ctx, eval, score(90)));

        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.AUTOMATIZACION
                && o.priority() >= 1));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.COMPETITIVA
                && o.priority() >= 1));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.FINANCIERA
                && o.priority() >= 1));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.INNOVACION
                && o.priority() >= 1));
    }

    // ---------------------------------------------------------------
    // Test 7c — oportunidades de sector y problema (mercado)
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaIdentificarOportunidadesDeSectorYProblema() {
        var ctx = new ProjectContext();
        ctx.update(new AnalysisResult(new LinkedHashMap<>(Map.of(
            AnalyzedDimension.TARGET_CUSTOMER, "Segmento definido",
            AnalyzedDimension.SOLUTION, "Solución con detalle técnico"))));
        var result = engine.evaluate(input(ctx, evaluation(0.4, 5, List.of()), score(60)));

        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.MERCADO
                && o.relatedDimension() == AnalyzedDimension.SECTOR));
        assertTrue(result.opportunities().stream()
            .anyMatch(o -> o.category() == OpportunityCategory.MERCADO
                && o.relatedDimension() == AnalyzedDimension.PROBLEM));
    }

    // ---------------------------------------------------------------
    // Test 8 — datos insuficientes
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaRetornarVacio_cuandoEntradaNula() {
        var result = engine.evaluate(null);
        assertFalse(result.hasOpportunities());
        assertEquals(0, result.opportunityCount());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoFaltanDatos() {
        var result = engine.evaluate(new OpportunityInput(null, null, null, null));
        assertFalse(result.hasOpportunities());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoScoreEsNulo() {
        var ctx = projectWith(Map.of());
        var result = engine.evaluate(new OpportunityInput(ctx,
            evaluation(0.3, 4, List.of()), ConversationDecision.generateReport("r"), null));
        assertFalse(result.hasOpportunities());
    }

    // ---------------------------------------------------------------
    // Test 9 — determinismo
    // ---------------------------------------------------------------
    @Test
    void evaluate_deberiaSerDeterminista() {
        var ctx = projectWith(Map.of(
            AnalyzedDimension.REVENUE_MODEL, "Modelo por suscripción",
            AnalyzedDimension.COMPETITION, "Competencia mapeada"));
        var in = input(ctx, evaluation(0.5, 6, List.of()), score(55));

        var r1 = engine.evaluate(in);
        var r2 = engine.evaluate(in);

        assertEquals(r1.opportunityCount(), r2.opportunityCount());
        for (int i = 0; i < r1.opportunities().size(); i++) {
            assertEquals(r1.opportunities().get(i).id(), r2.opportunities().get(i).id());
            assertEquals(r1.opportunities().get(i).title(), r2.opportunities().get(i).title());
            assertEquals(r1.opportunities().get(i).priority(), r2.opportunities().get(i).priority());
            assertEquals(r1.opportunities().get(i).confidence(), r2.opportunities().get(i).confidence(), 1e-9);
        }
        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.explanation(), r2.explanation());
    }

    // ---------------------------------------------------------------
    // Test 10 — auto-descubrimiento de analizadores
    // ---------------------------------------------------------------
    @Test
    void engine_deberiaDescubrirAnalizadoresRegistrados() {
        var engine2 = new OpportunityEngine(List.of(new MarketOpportunityAnalyzer()),
            OpportunityModel.defaultModel());
        assertEquals(1, engine2.analyzers().size());
        assertEquals(OpportunityCategory.MERCADO, engine2.analyzers().get(0).category());

        // Solo analiza oportunidades de la categoría registrada
        var ctx = projectWith(Map.of());
        var result = engine2.evaluate(input(ctx, evaluation(0.2, 3, List.of()), score(30)));
        assertTrue(result.hasOpportunities());
        assertTrue(result.opportunities().stream()
            .allMatch(o -> o.category() == OpportunityCategory.MERCADO));
    }
}
