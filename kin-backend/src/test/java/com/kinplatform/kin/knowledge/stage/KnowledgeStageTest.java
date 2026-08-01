package com.kinplatform.kin.knowledge.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeStageTest {

    private final CapturingSource capturingSource = new CapturingSource(validCandidate());
    private final KnowledgeStage dataStage = new KnowledgeStage(new KnowledgeEngine(
        new KnowledgeGateway(new SourceRegistry(List.of(capturingSource)), validator())));
    private final KnowledgeStage emptyStage = new KnowledgeStage(new KnowledgeEngine(
        new KnowledgeGateway(SourceRegistry.empty(), validator())));

    private SourceValidator validator() {
        return new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json", "text/plain"));
    }

    private KnowledgeCandidate validCandidate() {
        return new KnowledgeCandidate(
            "Mercado retail colombiano con crecimiento anual del 12%. Dato verificado.",
            "src-1", "Fuente", "https://example.com/report",
            OffsetDateTime.now().minusDays(30), "application/json",
            Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
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
    void name_deberiaSerConocimiento() {
        assertEquals("Conocimiento", emptyStage.name());
    }

    @Test
    void supports_deberiaSerFalso_cuandoContextoNulo() {
        assertFalse(emptyStage.supports(null));
    }

    @Test
    void supports_deberiaSerFalso_cuandoNoHayProjectContext() {
        var ctx = new PipelineContext(UUID.randomUUID(), UUID.randomUUID(), "m", List.of(), "t", "d", "c");
        assertFalse(emptyStage.supports(ctx));
    }

    @Test
    void supports_deberiaSerVerdadero_cuandoHayProjectContextValido() {
        assertTrue(emptyStage.supports(context(validContext())));
    }

    @Test
    void supports_deberiaSerVerdadero_conProjectContextVacio() {
        assertTrue(emptyStage.supports(context(emptyContext())));
    }

    @Test
    void execute_deberiaConstruirRequestDesdeProjectContextValido() {
        var ctx = context(validContext());

        var result = dataStage.execute(ctx);

        assertSame(ctx, result);
        assertNotNull(capturingSource.lastQuery);
        assertEquals("Tienda Online", capturingSource.lastQuery.topic());
        assertTrue(capturingSource.lastQuery.keywords().contains("Retail"));
        assertTrue(capturingSource.lastQuery.keywords().contains("Falta visibilidad de mercado"));
        assertEquals(KnowledgeRequest.DEFAULT_LIMIT, capturingSource.lastQuery.limit());
    }

    @Test
    void execute_deberiaUsarSolucionComoTema_cuandoNoHayNombre() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.SOLUTION, "App de entrega");
        data.put(AnalyzedDimension.SECTOR, "Logística");
        var pc = ProjectContext.restore(data, data.keySet(), null, 1, false);
        var ctx = context(pc);

        dataStage.execute(ctx);

        assertEquals("App de entrega", capturingSource.lastQuery.topic());
    }

    @Test
    void execute_deberiaAlmacenarKnowledgeResultConDatos() {
        var ctx = context(validContext());

        dataStage.execute(ctx);

        KnowledgeResult result = ctx.knowledgeResult();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.factCount());
        assertEquals(1, result.sourcesUsed().size());
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
        assertSame(result, ctx.engineResult(KnowledgeEngine.GENERATOR_NAME));
    }

    @Test
    void execute_deberiaAlmacenarKnowledgeResultVacio_conProjectContextVacio() {
        var ctx = context(emptyContext());

        emptyStage.execute(ctx);

        KnowledgeResult result = ctx.knowledgeResult();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.factCount());
        assertEquals(0.0, result.confidence(), 1e-9);
        assertTrue(((KnowledgeResult) ctx.engineResult(KnowledgeEngine.GENERATOR_NAME)).isEmpty());
    }

    @Test
    void execute_deberiaActualizarElPipelineContext() {
        var ctx = context(validContext());

        dataStage.execute(ctx);

        assertNotNull(ctx.knowledgeResult());
        assertTrue(ctx.engineResults().containsKey(KnowledgeEngine.GENERATOR_NAME));
    }

    private static class CapturingSource implements KnowledgeSource {
        private final KnowledgeCandidate candidate;
        private KnowledgeQuery lastQuery;

        private CapturingSource(KnowledgeCandidate candidate) {
            this.candidate = candidate;
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            this.lastQuery = query;
            return List.of(candidate);
        }
    }
}
