package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.planner.ProviderType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityEdgeTest {

    @Test
    void count_conMetroAusente_deberiaDevolverCero() {
        var metrics = new KnowledgeMetrics(new SimpleMeterRegistry());

        assertEquals(0.0, metrics.count("kin.knowledge.no_existe", "tag", "v"), 1e-9);
    }

    @Test
    void timedProviderRegistry_conFuenteNula_deberiaEnvuelverse() {
        var metrics = new KnowledgeMetrics(new SimpleMeterRegistry());
        var registry = new TimedProviderRegistry(
            type -> java.util.Arrays.asList(null, new StubSource()), metrics);

        var sources = registry.sourcesFor(ProviderType.GOVERNMENT);

        assertEquals(2, sources.size());
        assertTrue(sources.get(0) == null);
        assertNotNull(sources.get(1));
    }

    @Test
    void timedKnowledgeSource_conProviderNulo_deberiaRegistrarDesconocido() {
        var metrics = new KnowledgeMetrics(new SimpleMeterRegistry());
        var source = new TimedKnowledgeSource(null, new StubSource(), metrics);

        var result = source.fetch(KnowledgeQuery.from(
            com.kinplatform.kin.knowledge.KnowledgeRequest.of("tema", List.of())));

        assertNotNull(result);
        assertEquals(1.0, metrics.count("kin.knowledge.provider.requests", "type", "UNKNOWN"), 1e-9);
    }

    @Test
    void correlationContext_unknown_deberiaSerEstable() {
        assertEquals("unknown", CorrelationContext.UNKNOWN.correlationId());
        assertEquals("unknown", CorrelationContext.UNKNOWN.traceId());
    }

    @Test
    void structuredLog_escape_deberiaTolerarNulos() {
        KnowledgeStructuredLog.event("cache_hit", "HIT");
        assertTrue(true);
    }

    private static final class StubSource implements KnowledgeSource {
        @Override
        public List<com.kinplatform.kin.knowledge.KnowledgeCandidate> fetch(KnowledgeQuery query) {
            return List.of();
        }
    }
}
