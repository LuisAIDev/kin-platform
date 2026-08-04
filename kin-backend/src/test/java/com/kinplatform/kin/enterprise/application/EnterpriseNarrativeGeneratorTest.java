package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del generador narrativo (Fase 10, Milestone 3E): produce EXECUTIVE_REPORT
 * y DOFA con la prosa de la IA y, si la IA no responde, ensambla un documento
 * determinista con los datos reales (offline-first).
 */
class EnterpriseNarrativeGeneratorTest {

    private final EnterpriseDocumentAssembler assembler = new EnterpriseDocumentAssembler();

    private EnterpriseNarrativeGenerator generator(AIResponder aiResponder) {
        return new EnterpriseNarrativeGenerator(aiResponder, assembler,
            new EnterpriseNarrativePromptBuilder(assembler));
    }

    @Test
    void generate_conRespuestaIa_produceLosDosDocumentosConLaProsa() {
        var aiResponder = mock(AIResponder.class);
        when(aiResponder.respond(any(AIRequest.class)))
            .thenReturn("Narrativa del Executive Report", "Narrativa del DOFA");
        var score = EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);

        List<DocumentArtifact> documents = generator(aiResponder).generate(1,
            EngineTestFixtures.contextWithAll(), EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8), EngineTestFixtures.knowledge(0.8),
            EngineTestFixtures.riskResult(0.8), score, List.of());

        assertEquals(2, documents.size());
        assertEquals(DocumentType.EXECUTIVE_REPORT, documents.get(0).type());
        assertEquals(DocumentType.DOFA, documents.get(1).type());
        assertEquals("Narrativa del Executive Report", documents.get(0).content());
        assertEquals("Narrativa del DOFA", documents.get(1).content());
        assertTrue(documents.get(0).content().length() > 0);
    }

    @Test
    void generate_conIaEnBlanco_usaFallbackDeterminista() {
        var aiResponder = mock(AIResponder.class);
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("   ");
        var score = EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);

        List<DocumentArtifact> documents = generator(aiResponder).generate(1,
            EngineTestFixtures.contextWithAll(), EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8), EngineTestFixtures.knowledge(0.8),
            EngineTestFixtures.riskResult(0.8), score, List.of());

        assertEquals(2, documents.size());
        assertFalse(documents.get(0).content().isBlank());
        assertFalse(documents.get(1).content().isBlank());
        assertTrue(documents.get(0).content().contains("Resumen Ejecutivo"));
        assertTrue(documents.get(1).content().contains("Fortalezas"));
        assertTrue(documents.get(1).content().contains("Innovación de proceso"));
        assertTrue(documents.get(1).content().contains("Riesgo financiero"));
    }

    @Test
    void generate_conIaNula_usaFallbackDeterminista() {
        var aiResponder = mock(AIResponder.class);
        when(aiResponder.respond(any(AIRequest.class))).thenReturn(null);
        var score = EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);

        List<DocumentArtifact> documents = generator(aiResponder).generate(1,
            EngineTestFixtures.contextWithAll(), EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8), EngineTestFixtures.knowledge(0.8),
            EngineTestFixtures.riskResult(0.8), score, List.of());

        assertEquals(2, documents.size());
        assertFalse(documents.get(0).content().isBlank());
        assertTrue(documents.get(0).content().contains("65"));
    }
}
