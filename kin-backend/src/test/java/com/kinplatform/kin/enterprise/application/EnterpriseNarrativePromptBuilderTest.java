package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del constructor de prompts narrativos (Fase 10, Milestone 3E): los
 * prompts del Executive Report y del DOFA reciben los datos deterministas y
 * exigen las secciones/cuadrantes requeridos sin permitir inventar datos.
 */
class EnterpriseNarrativePromptBuilderTest {

    private final EnterpriseNarrativePromptBuilder builder =
        new EnterpriseNarrativePromptBuilder(new EnterpriseDocumentAssembler());

    @Test
    void executiveRequest_contieneDatosDeterministasYLasOchoSecciones() {
        var context = EngineTestFixtures.contextWithAll();
        var score = EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);

        AIRequest request = builder.executiveRequest(context,
            EngineTestFixtures.recommendations(0.8), EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8), EngineTestFixtures.riskResult(0.8),
            score, List.of());

        assertNotNull(request.userMessage());
        assertNotNull(request.systemPrompt());
        assertTrue(request.userMessage().contains("CONTEXTO DEL PROYECTO"));
        assertTrue(request.userMessage().contains("ENTERPRISE SCORE"));
        assertTrue(request.userMessage().contains("65"));
        assertTrue(request.userMessage().contains("1000000"), "TAM del KnowledgeResult debe viajar al prompt");
        assertTrue(request.userMessage().contains("Innovación de proceso"));
        assertTrue(request.userMessage().contains("Riesgo financiero"));
        assertTrue(request.userMessage().contains("RECOMENDACIONES"));

        assertTrue(request.systemPrompt().contains("Resumen Ejecutivo"));
        assertTrue(request.systemPrompt().contains("Análisis Estratégico"));
        assertTrue(request.systemPrompt().contains("Viabilidad"));
        assertTrue(request.systemPrompt().contains("Hallazgos"));
        assertTrue(request.systemPrompt().contains("Oportunidades"));
        assertTrue(request.systemPrompt().contains("Riesgos"));
        assertTrue(request.systemPrompt().contains("Conclusiones"));
        assertTrue(request.systemPrompt().contains("Recomendaciones"));
        assertTrue(request.systemPrompt().contains("NO inventes datos"));
    }

    @Test
    void dofaRequest_contieneDatosDeterministasYLosCuatroCuadrantes() {
        var context = EngineTestFixtures.contextWithAll();
        var score = EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);

        AIRequest request = builder.dofaRequest(context,
            EngineTestFixtures.opportunities(0.8), EngineTestFixtures.riskResult(0.8),
            EngineTestFixtures.knowledge(0.8), score);

        assertTrue(request.userMessage().contains("OPORTUNIDADES"));
        assertTrue(request.userMessage().contains("RIESGOS"));
        assertTrue(request.userMessage().contains("CONOCIMIENTO EXTERNO"));
        assertTrue(request.userMessage().contains("Innovación de proceso"));
        assertTrue(request.systemPrompt().contains("Fortalezas"));
        assertTrue(request.systemPrompt().contains("Debilidades"));
        assertTrue(request.systemPrompt().contains("Oportunidades"));
        assertTrue(request.systemPrompt().contains("Amenazas"));
        assertTrue(request.systemPrompt().contains("NO inventes hechos"));
    }

    @Test
    void executiveRequest_conDocumentos_embebeSuContenido() {
        var context = EngineTestFixtures.contextWithAll();
        var assembler = new EnterpriseDocumentAssembler();
        var score = EnterpriseScore.calculate(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 0.5);
        var documents = List.of(assembler.narrative(1,
            com.kinplatform.kin.enterprise.valueobjects.DocumentType.KPI,
            "contenido determinista del KPI", "KpiEngine", "1.0.0"));

        AIRequest request = builder.executiveRequest(context,
            EngineTestFixtures.recommendations(0.8), EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8), EngineTestFixtures.riskResult(0.8),
            score, documents);

        assertTrue(request.userMessage().contains("contenido determinista del KPI"));
    }
}
