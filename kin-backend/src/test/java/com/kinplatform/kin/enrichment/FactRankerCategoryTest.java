package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests por categoría de análisis (ADR-016, Etapa E3): cada categoría debe
 * mapear hechos con sus propios términos y no los de otras categorías.
 */
class FactRankerCategoryTest {

    private final FactRanker ranker = new FactRanker();
    private final OffsetDateTime ref = OffsetDateTime.parse("2026-08-01T00:00:00Z");

    private KnowledgeFact fact(String claim, String category, SourceTrust trust, String sourceId) {
        return KnowledgeFact.of(claim, sourceId, "https://example.com", ref.minusDays(10), trust, category);
    }

    private KnowledgeResult result(KnowledgeFact... facts) {
        return new KnowledgeResult(List.of(facts),
            java.util.Arrays.stream(facts).map(KnowledgeFact::sourceId).toList(),
            List.of(), 0.8, "", "K", "v1");
    }

    private EnrichmentInput input(KnowledgeResult k, EvidenceCategory category) {
        return EnrichmentInput.of(ProjectContext.fromProject("P", "D", "C"), k,
            Set.of(category), 0.0);
    }

    @Test
    void market_deberiaSeleccionarHechosDeMercado() {
        var k = result(
            fact("El mercado retail crece con nueva demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "s1"),
            fact("Barrera de entrada baja para competidores", "competencia", SourceTrust.OFFICIAL_PUBLIC, "s2"));

        var ranks = ranker.rank(input(k, EvidenceCategory.MARKET));

        assertEquals(1, ranks.size());
        assertEquals(EvidenceCategory.MARKET, ranks.get(0).category());
        assertEquals(1, ranks.get(0).size());
        assertEquals("El mercado retail crece con nueva demanda", ranks.get(0).top().get().fact().claim());
    }

    @Test
    void innovation_deberiaSeleccionarHechosDeInnovacion() {
        var k = result(
            fact("Patente de tecnología disruptiva registrada", "innovacion", SourceTrust.OFFICIAL_PUBLIC, "s1"),
            fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "s2"));

        var ranks = ranker.rank(input(k, EvidenceCategory.INNOVATION));

        assertEquals(1, ranks.size());
        assertEquals(EvidenceCategory.INNOVATION, ranks.get(0).category());
        assertEquals(1, ranks.get(0).size());
        assertEquals("Patente de tecnología disruptiva registrada", ranks.get(0).top().get().fact().claim());
    }

    @Test
    void financial_deberiaSeleccionarHechosFinancieros() {
        var k = result(
            fact("Rentabilidad y margen del sector en aumento", "financiero", SourceTrust.OFFICIAL_PUBLIC, "s1"),
            fact("Tendencia de demanda del consumidor", "mercado", SourceTrust.OFFICIAL_PUBLIC, "s2"));

        var ranks = ranker.rank(input(k, EvidenceCategory.FINANCIAL));

        assertEquals(1, ranks.size());
        assertEquals(EvidenceCategory.FINANCIAL, ranks.get(0).category());
        assertEquals(1, ranks.get(0).size());
        assertEquals("Rentabilidad y margen del sector en aumento", ranks.get(0).top().get().fact().claim());
    }

    @Test
    void competitive_deberiaSeleccionarHechosCompetitivos() {
        var k = result(
            fact("Nuevo competidor con barreras de entrada", "competencia", SourceTrust.OFFICIAL_PUBLIC, "s1"),
            fact("Patente de tecnología disruptiva", "innovacion", SourceTrust.OFFICIAL_PUBLIC, "s2"));

        var ranks = ranker.rank(input(k, EvidenceCategory.COMPETITIVE));

        assertEquals(1, ranks.size());
        assertEquals(EvidenceCategory.COMPETITIVE, ranks.get(0).category());
        assertEquals(1, ranks.get(0).size());
        assertEquals("Nuevo competidor con barreras de entrada", ranks.get(0).top().get().fact().claim());
    }

    @Test
    void score_deberiaReconocerTerminosPorCategoria() {
        var deMercado = fact("Tamaño del mercado en crecimiento", "mercado",
            SourceTrust.OFFICIAL_PUBLIC, "s1");
        var deFinanzas = fact("Rentabilidad financiera positiva", "financiero",
            SourceTrust.OFFICIAL_PUBLIC, "s2");
        var deInnovacion = fact("Innovación con patente", "innovacion",
            SourceTrust.OFFICIAL_PUBLIC, "s3");
        var deCompetencia = fact("Barrera competitiva alta", "competencia",
            SourceTrust.OFFICIAL_PUBLIC, "s4");

        assertTrue(ranker.score(deMercado, EvidenceCategory.MARKET, ref).value() > 0.0);
        assertTrue(ranker.score(deFinanzas, EvidenceCategory.FINANCIAL, ref).value() > 0.0);
        assertTrue(ranker.score(deInnovacion, EvidenceCategory.INNOVATION, ref).value() > 0.0);
        assertTrue(ranker.score(deCompetencia, EvidenceCategory.COMPETITIVE, ref).value() > 0.0);
    }

    @Test
    void score_deberiaSerCeroCuandoLaCategoriaNoAplica() {
        var deMercado = fact("Tamaño del mercado en crecimiento", "mercado",
            SourceTrust.OFFICIAL_PUBLIC, "s1");

        assertEquals(0.0, ranker.score(deMercado, EvidenceCategory.FINANCIAL, ref).value(), 1e-9);
        assertEquals(0.0, ranker.score(deMercado, EvidenceCategory.INNOVATION, ref).value(), 1e-9);
        assertEquals(0.0, ranker.score(deMercado, EvidenceCategory.COMPETITIVE, ref).value(), 1e-9);
    }

    @Test
    void score_deberiaReconocerTerminosEnInglesPorCategoria() {
        var deMercado = fact("Retail market demand growing in LATAM", "market",
            SourceTrust.OFFICIAL_PUBLIC, "s1");
        var deFinanzas = fact("Revenue and margins improving", "financial",
            SourceTrust.OFFICIAL_PUBLIC, "s2");
        var deCompetencia = fact("New competitor with entry barriers", "competition",
            SourceTrust.OFFICIAL_PUBLIC, "s3");

        assertTrue(ranker.score(deMercado, EvidenceCategory.MARKET, ref).value() > 0.0);
        assertTrue(ranker.score(deFinanzas, EvidenceCategory.FINANCIAL, ref).value() > 0.0);
        assertTrue(ranker.score(deCompetencia, EvidenceCategory.COMPETITIVE, ref).value() > 0.0);
    }
}
