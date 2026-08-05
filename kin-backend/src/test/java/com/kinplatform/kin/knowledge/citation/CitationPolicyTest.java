package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationPolicyTest {

    private KnowledgeFact fact(SourceTrust trust, String sourceId, String url) {
        return KnowledgeFact.of("Dato verificado de mercado.", sourceId, url,
            OffsetDateTime.now().minusDays(5), trust, "MERCADO");
    }

    @Test
    void hechoNulo_deberiaExcluirse() {
        var decision = new VerifiedCitationPolicy().decide(null);

        assertFalse(decision.included());
        assertTrue(decision.reason().contains("nulo"));
    }

    @Test
    void sinSourceMetadata_deberiaExcluirse() {
        assertFalse(new VerifiedCitationPolicy().decide(fact(SourceTrust.OFFICIAL_PUBLIC, "", "https://example.com/x")).included());
        assertFalse(new VerifiedCitationPolicy().decide(fact(SourceTrust.OFFICIAL_PUBLIC, "src-1", "")).included());
    }

    @Test
    void confianzaAlta_deberiaIncluirse() {
        var decision = new VerifiedCitationPolicy().decide(fact(SourceTrust.OFFICIAL_PUBLIC, "src-1", "https://example.com/x"));

        assertTrue(decision.included());
        assertEquals("src-1", decision.sourceId());
        assertEquals(1.0, decision.confidence(), 1e-9);
    }

    @Test
    void confianzaBaja_deberiaExcluirseConUmbral() {
        var policy = new VerifiedCitationPolicy(0.8);

        assertFalse(policy.decide(fact(SourceTrust.UNVERIFIED, "src-1", "https://example.com/x")).included());
        assertTrue(policy.decide(fact(SourceTrust.OFFICIAL_PUBLIC, "src-1", "https://example.com/x")).included());
    }

    @Test
    void umbralJustoEnElLimite_deberiaIncluirse() {
        var policy = new VerifiedCitationPolicy(0.7);

        assertTrue(policy.decide(fact(SourceTrust.SECONDARY, "src-1", "https://example.com/x")).included());
    }

    @Test
    void umbralFueraDeRango_deberiaAcotarse() {
        var policy = new VerifiedCitationPolicy(1.5);

        assertEquals(0.0, policy.decide(fact(SourceTrust.UNVERIFIED, "src-1", "https://example.com/x")).confidence(), 1e-9);
        assertFalse(policy.decide(fact(SourceTrust.UNVERIFIED, "src-1", "https://example.com/x")).included());
    }
}
