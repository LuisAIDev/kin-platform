package com.kinplatform.kin.knowledge.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.pipeline.PipelineContext;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Modo sombra (ADR-025, Fase 1): el {@code KnowledgeStage} ejecuta el motor
 * completo (la fuente se consulta) pero suprime {@code knowledgeResult} del
 * contexto → el {@code EnrichmentStage} recibe vacío (invisible para el usuario).
 */
class KnowledgeStageShadowTest {

    private final CapturingSource source = new CapturingSource(List.of(validCandidate()));

    private SourceValidator validator() {
        return new SourceValidator(
                Set.of("example.com"), Duration.ofDays(365), Set.of("application/json", "text/plain"));
    }

    private KnowledgeCandidate validCandidate() {
        return new KnowledgeCandidate(
                "Mercado retail colombiano con crecimiento anual del 12%. Dato verificado.",
                "src-1",
                "Fuente",
                "https://example.com/report",
                OffsetDateTime.now().minusDays(30),
                "application/json",
                Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    private ProjectContext projectContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.PROJECT_NAME, "Tienda Online");
        data.put(AnalyzedDimension.SECTOR, "Retail");
        return ProjectContext.restore(data, data.keySet(), null, 1, false);
    }

    private PipelineContext context() {
        var ctx = new PipelineContext(
                UUID.randomUUID(), UUID.randomUUID(), "mensaje", List.of(), "Proyecto", "Descripción", "Tecnología");
        ctx.projectContext(projectContext());
        return ctx;
    }

    private KnowledgeStage shadowStage() {
        return new KnowledgeStage(
                new KnowledgeEngine(new KnowledgeGateway(new SourceRegistry(List.of(source)), validator())), true);
    }

    private KnowledgeStage normalStage() {
        return new KnowledgeStage(
                new KnowledgeEngine(new KnowledgeGateway(new SourceRegistry(List.of(source)), validator())), false);
    }

    @Test
    void modoSombra_deberiaEjecutarElMotorPeroNoPropagarElResultado() {
        var ctx = context();
        shadowStage().execute(ctx);

        assertNull(ctx.knowledgeResult(), "en sombra no se propaga el resultado al enriquecimiento");
        assertEquals(1, source.calls(), "el motor sí debe ejecutarse (consulta real + métricas)");
    }

    @Test
    void modoNormal_deberiaPropagarElResultado() {
        var ctx = context();
        normalStage().execute(ctx);

        assertNotNull(ctx.knowledgeResult(), "en modo normal el resultado sí se propaga");
        assertTrue(ctx.knowledgeResult().factCount() >= 1);
        assertEquals(1, source.calls());
    }

    private static final class CapturingSource implements KnowledgeSource {
        private final List<KnowledgeCandidate> candidates;
        private int calls;

        private CapturingSource(List<KnowledgeCandidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            calls++;
            return candidates;
        }

        private int calls() {
            return calls;
        }
    }
}

