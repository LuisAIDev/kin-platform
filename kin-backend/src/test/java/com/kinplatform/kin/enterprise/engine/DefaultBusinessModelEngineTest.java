package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.engine.input.BusinessModelInput;
import com.kinplatform.kin.enterprise.engine.result.BusinessModelResult;
import com.kinplatform.kin.enterprise.valueobjects.LeanCanvas;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultBusinessModelEngineTest {

    private final DefaultBusinessModelEngine engine = new DefaultBusinessModelEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:BusinessModel", metadata.name());
        assertEquals(EnginePhase.EXPLANATION, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals("KIN Architecture Team", metadata.author());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        var result = engine.evaluate(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_conContextoNull_deberiaRetornarVacio() {
        var input = new BusinessModelInput(null,
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));
        var result = engine.evaluate(input);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_conContextoSinDimensiones_deberiaRetornarVacio() {
        var input = new BusinessModelInput(
            ProjectContext.fromProject("", "", ""),
            EngineTestFixtures.recommendationsEmpty(),
            EngineTestFixtures.opportunitiesEmpty(),
            EngineTestFixtures.knowledgeEmpty());
        var result = engine.evaluate(input);
        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_deberiaConstruirLeanCanvasConDatosReales() {
        var input = new BusinessModelInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        LeanCanvas canvas = result.canvas();
        assertNotNull(canvas);
        assertEquals(1, canvas.problem().size());
        assertEquals("Problema claro del cliente", canvas.problem().get(0));
        assertEquals("Pymes del sector retail", canvas.customerSegments().get(0));
        assertEquals("Ahorro de costes operativos", canvas.uniqueValueProposition().get(0));
        assertEquals("Plataforma SaaS de gestión", canvas.solution().get(0));
        assertEquals("Suscripción mensual", canvas.revenueStreams().get(0));
        assertFalse(canvas.keyMetrics().isEmpty());
        assertTrue(result.confidence() > 0.0);
        assertEquals("BusinessModelEngine", result.generatedBy());
        assertEquals("1.0.0", result.engineVersion());
        assertFalse(result.explanation().isBlank());
    }

    @Test
    void evaluate_sinContextoNiOportunidades_deberiaRellenarConPorDefinir() {
        var input = new BusinessModelInput(
            EngineTestFixtures.contextWithAll(),
            null,
            null,
            EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        LeanCanvas canvas = result.canvas();
        assertEquals("Por definir", canvas.channels().get(0));
        assertEquals("Por definir", canvas.costStructure().get(0));
        assertEquals("Por definir", canvas.unfairAdvantage().get(0));
        assertTrue(result.confidence() < 1.0);
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new BusinessModelInput(
            EngineTestFixtures.contextWithAll(),
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8));

        var first = engine.evaluate(input);
        var second = engine.evaluate(input);

        assertEquals(first, second);
    }

    @Test
    void evaluate_conDimensionesAdicionales_deberiaDividirPorLineas() {
        var map = new java.util.LinkedHashMap<AnalyzedDimension, String>();
        map.put(AnalyzedDimension.PROBLEM, "Problema uno\nProblema dos");
        map.put(AnalyzedDimension.TARGET_CUSTOMER, "Segmento A\nSegmento B\nSegmento C");
        var input = new BusinessModelInput(
            EngineTestFixtures.context(map),
            EngineTestFixtures.recommendationsEmpty(),
            EngineTestFixtures.opportunitiesEmpty(),
            EngineTestFixtures.knowledgeEmpty());

        var result = engine.evaluate(input);

        assertEquals(2, result.canvas().problem().size());
        assertEquals(3, result.canvas().customerSegments().size());
    }
}
