package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeEvidenceTest {

    @Test
    void deberiaExponerHechoYScore() {
        var fact = KnowledgeFact.of("Tendencia de mercado al alza", "src-1", "https://example.com",
            null, null, "mercado");
        var score = EvidenceScore.of(0.8, EvidenceCategory.MARKET, "Coincidencia");

        var evidence = new KnowledgeEvidence(fact, score);

        assertEquals(fact, evidence.fact());
        assertEquals(score, evidence.score());
        assertEquals(EvidenceCategory.MARKET, evidence.category());
        assertEquals(0.8, evidence.scoreValue(), 1e-9);
    }

    @Test
    void deberiaNormalizarScoreNulo() {
        var fact = KnowledgeFact.of("Hecho", "src-1", "https://example.com", null, null, "");

        var evidence = new KnowledgeEvidence(fact, null);

        assertEquals(0.0, evidence.scoreValue(), 1e-9);
        assertEquals(0.0, evidence.score().value(), 1e-9);
        assertNull(evidence.category());
    }

    @Test
    void deberiaSoportarHechoNulo() {
        var evidence = new KnowledgeEvidence(null, EvidenceScore.of(0.5, EvidenceCategory.MARKET, ""));

        assertNull(evidence.fact());
        assertEquals(EvidenceCategory.MARKET, evidence.category());
    }

    @Test
    void of_deberiaConstruirEvidenciaCanonica() {
        var fact = KnowledgeFact.of("Hecho", "src-1", "https://example.com", null, null, "");

        var evidence = new KnowledgeEvidence(fact, EvidenceScore.of(0.4, EvidenceCategory.MARKET, ""));

        assertEquals(0.4, evidence.scoreValue(), 1e-9);
    }
}
