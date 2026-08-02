package com.kinplatform.kin.enrichment.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.FactRanker;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrichmentStageTest {

    private final EnrichmentStage stage = new EnrichmentStage(new EnrichmentEngine(new FactRanker()));

    private KnowledgeResult knowledge(KnowledgeFact... facts) {
        return new KnowledgeResult(List.of(facts),
            java.util.Arrays.stream(facts).map(KnowledgeFact::sourceId).toList(),
            List.of(), 0.8, "conocimiento", "KnowledgeEngine", "v1");
    }

    private KnowledgeFact fact(String claim, String category) {
        return KnowledgeFact.of(claim, "src-" + category, "https://example.com",
            OffsetDateTime.now().minusDays(10), SourceTrust.OFFICIAL_PUBLIC, category);
    }

    private ProjectContext validContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.PROJECT_NAME, "Tienda Online");
        data.put(AnalyzedDimension.SECTOR, "Retail");
        data.put(AnalyzedDimension.PROBLEM, "Falta visibilidad de mercado");
        data.put(AnalyzedDimension.TARGET_CUSTOMER, "PYMEs");
        return ProjectContext.restore(data, data.keySet(), null, 3, false);
    }

    private ProjectContext emptyContext() {
        return ProjectContext.restore(new EnumMap<>(AnalyzedDimension.class), Set.of(), null, 0, false);
    }

    private PipelineContext context(ProjectContext projectContext) {
        var ctx = new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "mensaje", List.of(),
            "Proyecto", "Descripción", "Tecnología");
        ctx.projectContext(projectContext);
        return ctx;
    }

    @Test
    void name_deberiaSerEnriquecimiento() {
        assertEquals("Enriquecimiento", stage.name());
    }

    @Test
    void supports_deberiaSerFalso_cuandoContextoNulo() {
        assertFalse(stage.supports(null));
    }

    @Test
    void supports_deberiaSerFalso_cuandoNoHayProjectContext() {
        var ctx = new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
        assertFalse(stage.supports(ctx));
    }

    @Test
    void supports_deberiaSerVerdadero_cuandoHayProjectContextValido() {
        assertTrue(stage.supports(context(validContext())));
    }

    @Test
    void supports_deberiaSerVerdadero_conProjectContextVacio() {
        assertTrue(stage.supports(context(emptyContext())));
    }

    @Test
    void supports_deberiaSerVerdadero_sinKnowledgeResult() {
        var ctx = context(validContext());
        ctx.knowledgeResult(null);
        assertTrue(stage.supports(ctx));
    }

    @Test
    void execute_deberiaAlmacenarEnrichmentResultConDatos_cuandoHayConocimientoRelevante() {
        var ctx = context(validContext());
        ctx.knowledgeResult(knowledge(
            fact("El mercado retail crece con demanda del consumidor", "mercado")));

        var result = stage.execute(ctx);

        assertSame(ctx, result);
        EnrichmentResult enrichment = ctx.enrichmentResult();
        assertNotNull(enrichment);
        assertFalse(enrichment.isEmpty());
        assertEquals(1, enrichment.totalEvidence());
        assertTrue(enrichment.rankFor(EvidenceCategory.MARKET).isPresent());
        assertEquals(EnrichmentEngine.GENERATOR_NAME, enrichment.generatedBy());
        assertEquals(EnrichmentEngine.ENGINE_VERSION, enrichment.engineVersion());
        assertSame(enrichment, ctx.engineResult(EnrichmentEngine.GENERATOR_NAME));
    }

    @Test
    void execute_deberiaDegradarAVacio_cuandoNoHayKnowledgeResult() {
        var ctx = context(validContext());

        stage.execute(ctx);

        EnrichmentResult enrichment = ctx.enrichmentResult();
        assertNotNull(enrichment);
        assertTrue(enrichment.isEmpty());
        assertEquals(0, enrichment.totalEvidence());
        assertEquals(0.0, enrichment.confidence(), 1e-9);
        assertTrue(((EnrichmentResult) ctx.engineResult(EnrichmentEngine.GENERATOR_NAME)).isEmpty());
    }

    @Test
    void execute_deberiaDegradarAVacio_cuandoElConocimientoEstaVacio() {
        var ctx = context(validContext());
        ctx.knowledgeResult(KnowledgeResult.empty());

        stage.execute(ctx);

        EnrichmentResult enrichment = ctx.enrichmentResult();
        assertNotNull(enrichment);
        assertTrue(enrichment.isEmpty());
    }

    @Test
    void execute_deberiaDegradarAVacio_cuandoLosHechosNoSonRelevantes() {
        var ctx = context(validContext());
        ctx.knowledgeResult(knowledge(
            fact("Detalle operativo sin relación con el proyecto", "operativo")));

        stage.execute(ctx);

        assertTrue(ctx.enrichmentResult().isEmpty());
    }

    @Test
    void execute_deberiaActualizarElPipelineContext() {
        var ctx = context(validContext());
        ctx.knowledgeResult(knowledge(
            fact("El mercado retail crece con demanda del consumidor", "mercado")));

        stage.execute(ctx);

        assertNotNull(ctx.enrichmentResult());
        assertTrue(ctx.engineResults().containsKey(EnrichmentEngine.GENERATOR_NAME));
    }
}
