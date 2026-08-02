package com.kinplatform.kin.enrichment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceScoreTest {

    @Test
    void deberiaNormalizarRazonNula() {
        var score = new EvidenceScore(0.5, EvidenceCategory.MARKET, null);

        assertEquals("", score.reason());
    }

    @Test
    void deberiaAcotarElValorAlRangoCeroUno() {
        assertEquals(0.0, new EvidenceScore(-0.3, EvidenceCategory.MARKET, "").value(), 1e-9);
        assertEquals(1.0, new EvidenceScore(1.8, EvidenceCategory.MARKET, "").value(), 1e-9);
    }

    @Test
    void of_deberiaConstruirScoreCanonico() {
        var score = EvidenceScore.of(0.7, EvidenceCategory.INNOVATION, "razón");

        assertEquals(0.7, score.value(), 1e-9);
        assertEquals(EvidenceCategory.INNOVATION, score.category());
        assertEquals("razón", score.reason());
    }

    @Test
    void isRelevant_deberiaCompararContraElUmbral() {
        var score = EvidenceScore.of(0.7, EvidenceCategory.MARKET, "");

        assertTrue(score.isRelevant(0.5));
        assertTrue(score.isRelevant(0.7));
        assertFalse(score.isRelevant(0.8));
    }

    @Test
    void deberiaPermitirCategoriaNula() {
        var score = new EvidenceScore(0.0, null, "Categoría nula.");

        assertNull(score.category());
    }
}
