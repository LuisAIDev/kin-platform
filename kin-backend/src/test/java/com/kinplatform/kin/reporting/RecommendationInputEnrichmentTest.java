package com.kinplatform.kin.reporting;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.EvidenceScore;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationInputEnrichmentTest {

    @Test
    void constructor4Parametros_deberiaNormalizarEnrichmentVacio() {
        var input = new RecommendationInput(null, null, null, null);
        assertNotNull(input.enrichment());
        assertTrue(input.enrichment().isEmpty());
    }

    @Test
    void constructor5Parametros_deberiaConservarEnrichment() {
        var enrichment = someEnrichment();
        var input = new RecommendationInput(null, null, null, null, enrichment);
        assertEquals(enrichment, input.enrichment());
    }

    @Test
    void constructor5Parametros_conNull_deberiaNormalizarVacio() {
        var input = new RecommendationInput(null, null, null, null, null);
        assertNotNull(input.enrichment());
        assertTrue(input.enrichment().isEmpty());
    }

    @Test
    void withEnrichment_deberiaCrearNuevaInstanciaConservandoCampos() {
        var ctx = ProjectContext.fromProject("Proyecto", "Descripción", "Tecnología");
        var eval = CompletenessEvaluation.empty();
        var score = ScoreResult.empty();
        var base = new RecommendationInput(ctx, eval,
            ConversationDecision.generateReport("reporte"), score);

        var enriched = base.withEnrichment(someEnrichment());

        assertEquals(ctx, enriched.projectContext());
        assertEquals(eval, enriched.evaluation());
        assertEquals(score, enriched.score());
        assertEquals(ConversationDecision.generateReport("reporte"), enriched.decision());
        assertFalse(enriched.enrichment().isEmpty());
        assertTrue(base.enrichment().isEmpty());
    }

    private static EnrichmentResult someEnrichment() {
        var fact = KnowledgeFact.of("El mercado crece 15% anual", "src-1", "https://example.com",
            OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "sector");
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(new KnowledgeEvidence(fact,
                EvidenceScore.of(0.8, EvidenceCategory.MARKET, "Relevante."))));
        return new EnrichmentResult(List.of(rank), List.of("src-1"), 0.8,
            "enriquecido", "Test", "v1");
    }
}
