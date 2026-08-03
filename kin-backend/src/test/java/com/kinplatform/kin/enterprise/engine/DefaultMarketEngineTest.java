package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.MarketInput;
import com.kinplatform.kin.enterprise.engine.result.MarketResult;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMarketEngineTest {

    private final DefaultMarketEngine engine = new DefaultMarketEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:Market", metadata.name());
        assertEquals(EnginePhase.MARKET, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_conContextoNull_deberiaRetornarVacio() {
        var input = new MarketInput(null,
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_enModoOffline_deberiaCeroYPorDefinir() {
        var input = new MarketInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendationsEmpty(),
            EngineTestFixtures.opportunitiesEmpty(),
            EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        MarketPlan plan = result.plan();
        assertEquals(0.0, plan.tam());
        assertEquals(0.0, plan.sam());
        assertEquals(0.0, plan.som());
        assertEquals(0.0, plan.growthRate());
        assertEquals(0.0, result.confidence());
        assertTrue(plan.channels().get(0).equals("Por definir"));
        assertEquals("Pymes del sector retail", plan.customerSegments().get(0));
        assertTrue(result.explanation().contains("offline"));
    }

    @Test
    void evaluate_conConocimiento_deberiaExtraerTamSamSom() {
        var input = new MarketInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.9));

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        MarketPlan plan = result.plan();
        assertEquals(1000000.0, plan.tam());
        assertEquals(15.0, plan.sam());
        assertEquals(0.0, plan.som());
        assertEquals(15.0, plan.growthRate());
        assertEquals(0.9, result.confidence());
        assertFalse(plan.competitors().isEmpty());
    }

    @Test
    void evaluate_conCompetenciaEnContexto_deberiaIncluirla() {
        var map = new java.util.LinkedHashMap<AnalyzedDimension, String>();
        map.put(AnalyzedDimension.COMPETITION, "Competidor A");
        map.put(AnalyzedDimension.TARGET_CUSTOMER, "Segmento único");
        var input = new MarketInput(
            EngineTestFixtures.context(map),
            null, null, EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        MarketPlan plan = result.plan();
        assertEquals("Competidor A", plan.competitors().get(0));
        assertEquals("Segmento único", plan.customerSegments().get(0));
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new MarketInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.9));

        assertEquals(engine.evaluate(input), engine.evaluate(input));
    }

    @Test
    void evaluate_conKnowledgeNull_deberiaTratarComoOffline() {
        var input = new MarketInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendationsEmpty(),
            EngineTestFixtures.opportunitiesEmpty(),
            null);

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        assertEquals(0.0, result.confidence());
        assertEquals(0.0, result.plan().tam());
        assertEquals("Por definir", result.plan().channels().get(0));
        assertEquals("Por definir", result.plan().entryBarriers().get(0));
        assertTrue(result.explanation().contains("offline"));
    }

    @Test
    void evaluate_sinClaimDeCrecimiento_deberiaUsarPrimerNumero() {
        var knowledge = new com.kinplatform.kin.knowledge.KnowledgeResult(
            List.of(
                com.kinplatform.kin.knowledge.KnowledgeFact.of(
                    "El mercado alcanza 1000000 unidades", "src1",
                    "https://example.com/a", java.time.OffsetDateTime.now(),
                    com.kinplatform.kin.knowledge.SourceTrust.OFFICIAL_PUBLIC, "mercado"),
                com.kinplatform.kin.knowledge.KnowledgeFact.of(
                    "Dato descriptivo sin cifras", "src2",
                    "https://example.com/b", java.time.OffsetDateTime.now(),
                    com.kinplatform.kin.knowledge.SourceTrust.SECONDARY, "contexto")),
            List.of("src1", "src2"), List.of(), 0.7,
            "Conocimiento.", "KnowledgeEngine", "1.0.0");
        var input = new MarketInput(
            EngineTestFixtures.contextWithAll(),
            null, null, knowledge);

        var result = engine.evaluate(input);

        assertEquals(1000000.0, result.plan().tam());
        assertEquals(1000000.0, result.plan().growthRate());
        assertEquals(0.0, result.plan().sam());
    }

    @Test
    void evaluate_conClaimSinNumeros_deberiaUsarCero() {
        var input = new MarketInput(
            EngineTestFixtures.contextWithAll(),
            null, null, EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertEquals(0.0, result.plan().tam());
        assertEquals("MarketEngine", result.generatedBy());
        assertEquals("1.0.0", result.engineVersion());
    }

    @Test
    void context_sinDimensiones_peroCubierto_deberiaProcesar() {
        var map = new java.util.LinkedHashMap<AnalyzedDimension, String>();
        map.put(AnalyzedDimension.CITY, "Buenos Aires");
        var input = new MarketInput(
            EngineTestFixtures.context(map),
            EngineTestFixtures.recommendationsEmpty(),
            EngineTestFixtures.opportunitiesEmpty(),
            EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        assertEquals("Por definir", result.plan().customerSegments().get(0));
    }
}
