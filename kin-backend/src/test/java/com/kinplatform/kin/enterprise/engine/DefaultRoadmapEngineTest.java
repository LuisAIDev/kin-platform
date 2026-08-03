package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.RoadmapInput;
import com.kinplatform.kin.enterprise.engine.result.RoadmapResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.Roadmap;
import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationCategory;
import com.kinplatform.kin.reporting.RecommendationExplanation;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.reporting.EffortLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRoadmapEngineTest {

    private final DefaultRoadmapEngine engine = new DefaultRoadmapEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:Roadmap", metadata.name());
        assertEquals(EnginePhase.EXPLANATION, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_conContextoNull_deberiaRetornarVacio() {
        var input = new RoadmapInput(null, recommendations(), financialPlan());
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_sinRecomendaciones_deberiaRoadmapPorDefecto() {
        var input = new RoadmapInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendationsEmpty(),
            financialPlan());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        Roadmap roadmap = result.roadmap();
        assertFalse(roadmap.phases().isEmpty());
        assertEquals("Por definir", roadmap.timeline());
        assertEquals(0.0, result.confidence());
    }

    @Test
    void evaluate_conRecomendaciones_deberiaFasesOrdenadas() {
        var input = new RoadmapInput(
            EngineTestFixtures.contextWithAll(),
            recommendations(),
            financialPlan());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        Roadmap roadmap = result.roadmap();
        assertTrue(roadmap.phases().contains("product"));
        assertTrue(roadmap.phases().contains("financial"));
        assertEquals(roadmap.phases().size(), roadmap.milestones().size());
        assertEquals(roadmap.phases().size(), roadmap.ganttEntries().size());
        assertFalse(roadmap.dependencies().isEmpty());
        assertEquals(0.9, result.confidence());
        assertTrue(roadmap.timeline().contains("meses"));
    }

    @Test
    void evaluate_conBreakEven_deberiaAlinearHorizonte() {
        var plan = FinancialPlan.of(250.0, 400.0, 1000.0, 1100.0, 1210.0, 18, 60.0,
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0));
        var input = new RoadmapInput(
            EngineTestFixtures.contextWithAll(),
            recommendations(),
            plan);

        var result = engine.evaluate(input);

        assertTrue(result.roadmap().timeline().contains("18"));
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new RoadmapInput(
            EngineTestFixtures.contextWithAll(),
            recommendations(),
            financialPlan());

        assertEquals(engine.evaluate(input), engine.evaluate(input));
    }

    private RecommendationResult recommendations() {
        var product = Recommendation.create(RecommendationCategory.PRODUCT, "Construir MVP",
            "Descripción", 9, ImpactLevel.HIGH, EffortLevel.MEDIUM, AnalyzedDimension.MVP,
            List.of("Iterar"), "MVP funcional",
            RecommendationExplanation.of(List.of(), "regla", "razón"));
        var financial = Recommendation.create(RecommendationCategory.FINANCIAL, "Modelo financiero",
            "Descripción", 7, ImpactLevel.MEDIUM, EffortLevel.LOW, AnalyzedDimension.OBJECTIVES,
            List.of("Proyectar"), "Sostenibilidad",
            RecommendationExplanation.of(List.of(), "regla", "razón"));
        return new RecommendationResult(List.of(product, financial), 9, 0.9,
            RecommendationCategory.PRODUCT, "Recomendaciones.", "RecommendationEngine", "1.0.0");
    }

    private FinancialPlan financialPlan() {
        return FinancialPlan.of(250.0, 400.0, 1000.0, 1100.0, 1210.0, 5, 60.0,
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0));
    }
}
