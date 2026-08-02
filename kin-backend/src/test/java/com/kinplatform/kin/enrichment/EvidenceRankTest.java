package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceRankTest {

    private KnowledgeEvidence evidence(double value, String claim) {
        var fact = KnowledgeFact.of(claim, "src-" + value, "https://example.com", null, null, "mercado");
        return new KnowledgeEvidence(fact, EvidenceScore.of(value, EvidenceCategory.MARKET, ""));
    }

    @Test
    void deberiaNormalizarListaNula() {
        var rank = new EvidenceRank(EvidenceCategory.MARKET, null, 0.8);

        assertTrue(rank.isEmpty());
        assertEquals(0, rank.size());
    }

    @Test
    void deberiaOrdenarDeMayorAMenorScore() {
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(evidence(0.3, "bajo"), evidence(0.9, "alto"), evidence(0.6, "medio")));

        assertEquals(3, rank.size());
        assertEquals(0.9, rank.evidence().get(0).scoreValue(), 1e-9);
        assertEquals(0.6, rank.evidence().get(1).scoreValue(), 1e-9);
        assertEquals(0.3, rank.evidence().get(2).scoreValue(), 1e-9);
    }

    @Test
    void deberiaCalcularConfianzaComoPromedio() {
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(evidence(0.8, "a"), evidence(0.6, "b")));

        assertEquals(0.7, rank.confidence(), 1e-9);
    }

    @Test
    void deberiaDevolverConfianzaCeroSinEvidencia() {
        var rank = EvidenceRank.of(EvidenceCategory.MARKET, List.of());

        assertEquals(0.0, rank.confidence(), 1e-9);
        assertTrue(rank.isEmpty());
    }

    @Test
    void deberiaExponerLaEvidenciaPrincipal() {
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(evidence(0.9, "principal"), evidence(0.4, "secundaria")));

        assertTrue(rank.top().isPresent());
        assertEquals("principal", rank.top().get().fact().claim());
        assertFalse(rank.isEmpty());
        assertEquals(EvidenceCategory.MARKET, rank.category());
    }

    @Test
    void deberiaAcotarConfianzaAlRango() {
        assertEquals(0.0, new EvidenceRank(EvidenceCategory.MARKET, List.of(), -1.0).confidence(), 1e-9);
        assertEquals(1.0, new EvidenceRank(EvidenceCategory.MARKET, List.of(), 1.5).confidence(), 1e-9);
    }
}
