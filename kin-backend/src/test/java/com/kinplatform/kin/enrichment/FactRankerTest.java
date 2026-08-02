package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactRankerTest {

    private final FactRanker ranker = new FactRanker();

    private KnowledgeFact fact(String claim, String category, SourceTrust trust, String sourceId) {
        return KnowledgeFact.of(claim, sourceId, "https://example.com", null, trust, category);
    }

    private EnrichmentInput input(KnowledgeResult knowledge, Set<EvidenceCategory> categories,
                                  double minScore) {
        return EnrichmentInput.of(ProjectContext.fromProject("P", "D", "C"), knowledge, categories, minScore);
    }

    @Test
    void score_deberiaPuntuarPorCoincidenciaDeTerminosYConfianza() {
        var f = fact("El mercado minorista muestra alta demanda de retail", "mercado",
            SourceTrust.OFFICIAL_PUBLIC, "src-1");

        var score = ranker.score(f, EvidenceCategory.MARKET);

        assertTrue(score.value() > 0.0);
        assertEquals(EvidenceCategory.MARKET, score.category());
        assertTrue(score.reason().contains("Coincide"));
    }

    @Test
    void score_deberiaSerCeroSinCoincidenciaDeTerminos() {
        var f = fact("Dato irrelevante sobre pintura", "arte", SourceTrust.OFFICIAL_PUBLIC, "src-1");

        var score = ranker.score(f, EvidenceCategory.FINANCIAL);

        assertEquals(0.0, score.value(), 1e-9);
        assertTrue(score.reason().contains("Sin coincidencia"));
    }

    @Test
    void score_deberiaPremiarFuentesOficiales() {
        var oficial = fact("El ingreso y el margen del sector retail crecen", "financiero",
            SourceTrust.OFFICIAL_PUBLIC, "src-1");
        var noVerificada = fact("El ingreso y el margen del sector retail crecen", "financiero",
            SourceTrust.UNVERIFIED, "src-2");

        var sOficial = ranker.score(oficial, EvidenceCategory.FINANCIAL);
        var sNoVerificada = ranker.score(noVerificada, EvidenceCategory.FINANCIAL);

        assertTrue(sOficial.value() > sNoVerificada.value());
    }

    @Test
    void score_deberiaManejarHechoYCategoriaNulos() {
        assertEquals(0.0, ranker.score(null, EvidenceCategory.MARKET).value(), 1e-9);
        assertEquals(0.0, ranker.score(fact("x", "y", SourceTrust.OFFICIAL_PUBLIC, "s"), null).value(), 1e-9);
    }

    @Test
    void rank_deberiaAgruparPorCategoriaYOrdenar() {
        var conocimiento = new KnowledgeResult(List.of(
            fact("El mercado retail crece con nueva demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Barrera de entrada baja para competidores", "competencia", SourceTrust.OFFICIAL_PUBLIC, "src-2"),
            fact("Sin relación alguna", "otro", SourceTrust.UNVERIFIED, "src-3")
        ), List.of("src-1", "src-2", "src-3"), List.of(), 0.8, "", "K", "v1");

        var ranks = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET, EvidenceCategory.COMPETITIVE), 0.0));

        assertEquals(2, ranks.size());
        var market = ranks.stream().filter(r -> r.category() == EvidenceCategory.MARKET).findFirst().orElseThrow();
        assertTrue(market.size() >= 1);
        assertTrue(market.top().isPresent());
    }

    @Test
    void rank_deberiaAplicarElUmbralMinimoDeRelevancia() {
        var conocimiento = new KnowledgeResult(List.of(
            fact("El mercado retail crece con nueva demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1")
        ), List.of("src-1"), List.of(), 0.8, "", "K", "v1");

        var ranksAlto = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET), 0.99));
        assertEquals(0, ranksAlto.size());

        var ranksBajo = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET), 0.0));
        assertEquals(1, ranksBajo.size());
    }

    @Test
    void rank_deberiaRespetarElLimitePorCategoria() {
        var conocimiento = new KnowledgeResult(List.of(
            fact("El mercado retail crece", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Nueva demanda de mercado", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-2"),
            fact("El sector de mercado es clave", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-3")
        ), List.of("src-1", "src-2", "src-3"), List.of(), 0.8, "", "K", "v1");

        var ranks = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET), 0.0), 2);

        assertEquals(1, ranks.size());
        assertEquals(2, ranks.get(0).size());
    }

    @Test
    void rank_deberiaEliminarDuplicadosPorHecho() {
        var f = fact("El mercado retail crece con demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1");
        var conocimiento = new KnowledgeResult(List.of(f, f), List.of("src-1"), List.of(), 0.8, "", "K", "v1");

        var ranks = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET), 0.0));

        assertEquals(1, ranks.get(0).size());
    }

    @Test
    void rank_deberiaDevolverVacioConConocimientoVacioONulo() {
        var vacio = new KnowledgeResult(List.of(), List.of(), List.of(), 0.0, "", "K", "v1");
        assertTrue(ranker.rank(input(vacio, Set.of(EvidenceCategory.MARKET), 0.0)).isEmpty());
        assertTrue(ranker.rank(new EnrichmentInput(null, null, null, 0.0)).isEmpty());
    }

    @Test
    void rank_deberiaSerDeterminista() {
        var conocimiento = new KnowledgeResult(List.of(
            fact("El mercado retail crece con demanda", "mercado", SourceTrust.OFFICIAL_PUBLIC, "src-1"),
            fact("Barrera de entrada baja", "competencia", SourceTrust.SECONDARY, "src-2")
        ), List.of("src-1", "src-2"), List.of(), 0.8, "", "K", "v1");

        var r1 = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET), 0.0));
        var r2 = ranker.rank(input(conocimiento, Set.of(EvidenceCategory.MARKET), 0.0));

        assertEquals(r1, r2);
        assertFalse(r1.isEmpty());
    }

    @Test
    void score_deberiaReconocerTerminosEnIngles() {
        var f = fact("Retail market and demand trends in LATAM", "market",
            SourceTrust.OFFICIAL_PUBLIC, "src-1");

        var score = ranker.score(f, EvidenceCategory.MARKET);

        assertTrue(score.value() > 0.0);
    }
}
