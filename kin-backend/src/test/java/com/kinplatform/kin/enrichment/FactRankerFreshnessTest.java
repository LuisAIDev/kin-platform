package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de frescura del {@link FactRanker} (ADR-016, Etapa E3): la edad del
 * dato (respecto a un tiempo de referencia determinista) debe ponderar el
 * score; datos recientes puntúan más alto que datos antiguos.
 */
class FactRankerFreshnessTest {

    private static final OffsetDateTime REF = OffsetDateTime.parse("2026-08-01T00:00:00Z");

    private final FactRanker ranker = new FactRanker(0.5, 0.3, 0.2, REF);

    private KnowledgeFact fact(String claim, OffsetDateTime publishedAt) {
        return KnowledgeFact.of(claim, "src-1", "https://example.com", publishedAt,
            SourceTrust.OFFICIAL_PUBLIC, "mercado");
    }

    @Test
    void freshnessFactor_deberiaPonderarPorEdadDelDato() {
        assertEquals(1.0, FactRanker.freshnessFactor(REF.minusDays(5), REF), 1e-9);
        assertEquals(0.8, FactRanker.freshnessFactor(REF.minusDays(60), REF), 1e-9);
        assertEquals(0.6, FactRanker.freshnessFactor(REF.minusDays(150), REF), 1e-9);
        assertEquals(0.4, FactRanker.freshnessFactor(REF.minusDays(300), REF), 1e-9);
        assertEquals(0.2, FactRanker.freshnessFactor(REF.minusDays(500), REF), 1e-9);
    }

    @Test
    void freshnessFactor_conDatoSinFecha_deberiaSerNeutro() {
        assertEquals(0.5, FactRanker.freshnessFactor(null, REF), 1e-9);
        assertEquals(0.5, FactRanker.freshnessFactor(REF.minusDays(10), null), 1e-9);
    }

    @Test
    void freshnessFactor_deberiaSerToleranteAFechaFutura() {
        assertEquals(1.0, FactRanker.freshnessFactor(REF.plusDays(10), REF), 1e-9);
    }

    @Test
    void score_deberiaPremiarDatosRecientesSobreAntiguos() {
        var reciente = fact("El mercado crece con demanda", REF.minusDays(10));
        var antiguo = fact("El mercado crece con demanda", REF.minusDays(400));

        var sReciente = ranker.score(reciente, EvidenceCategory.MARKET);
        var sAntiguo = ranker.score(antiguo, EvidenceCategory.MARKET);

        assertTrue(sReciente.value() > sAntiguo.value());
        assertEquals(0.5 * (2.0 / 3.0) + 0.3 * 1.0 + 0.2 * 1.0, sReciente.value(), 1e-9);
        assertEquals(0.5 * (2.0 / 3.0) + 0.3 * 1.0 + 0.2 * 0.2, sAntiguo.value(), 1e-9);
    }

    @Test
    void score_deberiaSerDeterministaConTiempoDeReferenciaFijo() {
        var f = fact("El mercado crece con demanda", REF.minusDays(60));

        var s1 = ranker.score(f, EvidenceCategory.MARKET);
        var s2 = ranker.score(f, EvidenceCategory.MARKET);

        assertEquals(s1, s2);
    }

    @Test
    void score_conReferenciaNula_deberiaSerCero() {
        var f = fact("El mercado crece con demanda", REF.minusDays(10));

        assertEquals(0.0, ranker.score(f, EvidenceCategory.MARKET, null).value(), 1e-9);
    }

    @Test
    void score_deberiaExponerLaFrescuraEnLaRazon() {
        var reciente = fact("El mercado crece con demanda", REF.minusDays(10));

        var score = ranker.score(reciente, EvidenceCategory.MARKET);

        assertTrue(score.reason().contains("frescura"));
    }
}
