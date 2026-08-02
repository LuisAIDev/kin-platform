package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.EvidenceScore;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineContextTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private PipelineContext context() {
        return new PipelineContext(PROJECT_ID, USER_ID, "mensaje", List.of(),
            "Proyecto", "Descripción", "Tecnología");
    }

    private EnrichmentResult enrichment() {
        var fact = KnowledgeFact.of("El mercado crece con demanda", "src-1",
            "https://example.com", OffsetDateTime.now(), SourceTrust.OFFICIAL_PUBLIC, "mercado");
        var rank = EvidenceRank.of(EvidenceCategory.MARKET,
            List.of(new KnowledgeEvidence(fact, EvidenceScore.of(0.8, EvidenceCategory.MARKET, "Relevante."))));
        return new EnrichmentResult(List.of(rank), List.of("src-1"), 0.8,
            "enriquecido", "Test", "v1");
    }

    @Test
    void constructorDeCompatibilidad_deberiaInicializarCamposPrevios() {
        var ctx = context();

        assertEquals(PROJECT_ID, ctx.projectId());
        assertEquals(USER_ID, ctx.userId());
        assertEquals("mensaje", ctx.userMessage());
        assertEquals("Proyecto", ctx.projectTitle());
        assertEquals("Descripción", ctx.projectDescription());
        assertEquals("Tecnología", ctx.projectCategory());
        assertTrue(ctx.history().isEmpty());
    }

    @Test
    void constructorDeCompatibilidad_deberiaDejarEnrichmentResultNulo() {
        assertNull(context().enrichmentResult());
    }

    @Test
    void withEnrichmentResult_deberiaAlmacenarElResultado() {
        var ctx = context();
        var enrichment = enrichment();

        ctx.withEnrichmentResult(enrichment);

        assertSame(enrichment, ctx.enrichmentResult());
    }

    @Test
    void withEnrichmentResult_conNulo_deberiaResetearElCampo() {
        var ctx = context();
        ctx.withEnrichmentResult(enrichment());

        ctx.withEnrichmentResult(null);

        assertNull(ctx.enrichmentResult());
    }

    @Test
    void constructorAditivo_deberiaInicializarElEnrichmentResult() {
        var enrichment = enrichment();

        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "mensaje", List.of(),
            "Proyecto", "Descripción", "Tecnología", enrichment);

        assertSame(enrichment, ctx.enrichmentResult());
        assertEquals(PROJECT_ID, ctx.projectId());
    }

    @Test
    void constructorAditivo_conEnrichmentNulo_deberiaMantenerCampoNulo() {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "mensaje", List.of(),
            "Proyecto", "Descripción", "Tecnología", null);

        assertNull(ctx.enrichmentResult());
    }

    @Test
    void enrichmentResult_deberiaSerIndependienteDeLosOtrosCamposAditivos() {
        var ctx = context();
        var interview = com.kinplatform.kin.interview.InterviewResult.empty();
        var knowledge = com.kinplatform.kin.knowledge.KnowledgeResult.empty();
        var enrichment = enrichment();
        ctx.interviewResult(interview);
        ctx.knowledgeResult(knowledge);
        ctx.withEnrichmentResult(enrichment);

        assertSame(interview, ctx.interviewResult());
        assertSame(knowledge, ctx.knowledgeResult());
        assertSame(enrichment, ctx.enrichmentResult());
    }

    @Test
    void enrichmentResult_deberiaSerAditivoAlFlujoDeDatosDelPipeline() {
        var ctx = context();
        ctx.projectContext(com.kinplatform.kin.context.ProjectContext.fromProject("P", "D", "C"));
        ctx.withEnrichmentResult(com.kinplatform.kin.enrichment.EnrichmentResult.empty());

        assertEquals("P", ctx.projectContext().value(AnalyzedDimension.PROJECT_NAME));
        assertTrue(ctx.enrichmentResult().isEmpty());
    }
}
