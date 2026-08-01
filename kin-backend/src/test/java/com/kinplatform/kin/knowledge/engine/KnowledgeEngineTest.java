package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeEngineTest {

    private KnowledgeGateway gatewayWith(String... sourceIds) {
        var sources = java.util.Arrays.stream(sourceIds)
            .map(id -> (KnowledgeSource) new FixedSource(validCandidate(id)))
            .toList();
        return new KnowledgeGateway(new SourceRegistry(sources), validator());
    }

    private static class FixedSource implements KnowledgeSource {
        private final List<KnowledgeCandidate> candidates;

        private FixedSource(KnowledgeCandidate candidate) {
            this.candidates = List.of(candidate);
        }

        @Override
        public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
            return candidates;
        }
    }

    private SourceValidator validator() {
        return new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json"));
    }

    private KnowledgeCandidate validCandidate(String sourceId) {
        return new KnowledgeCandidate("Dato verificado de mercado. ".repeat(12), sourceId, "Fuente",
            "https://example.com/report", OffsetDateTime.now().minusDays(30),
            "application/json", Map.of(SourceValidator.META_SOURCE_TYPE, "official"));
    }

    @Test
    void metadata_deberiaDeclararFaseTipoYPrioridadCorrectos() {
        var engine = new KnowledgeEngine(gatewayWith("src-1"));
        var metadata = engine.metadata();

        assertEquals("KnowledgeEngine", metadata.name());
        assertEquals("v1", metadata.version());
        assertEquals("KIN Architecture Team", metadata.author());
        assertEquals(EnginePhase.KNOWLEDGE, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals(50, metadata.priority());
        assertTrue(metadata.dependencies().isEmpty());
    }

    @Test
    void evaluate_deberiaDelegarAlGateway_y_ProducirResultadoCompleto() {
        var engine = new KnowledgeEngine(gatewayWith("src-1"));

        var result = engine.evaluate(KnowledgeInput.of(
            KnowledgeRequest.of("mercado", List.of("retail"))));

        assertFalse(result.isEmpty());
        assertEquals(1, result.factCount());
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(KnowledgeEngine.ENGINE_VERSION, result.engineVersion());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoEntradaNula() {
        var engine = new KnowledgeEngine(gatewayWith("src-1"));

        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_deberiaRetornarVacio_cuandoElRequestEsNulo() {
        var engine = new KnowledgeEngine(gatewayWith("src-1"));

        var result = engine.evaluate(new KnowledgeInput(null));

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_conGatewayNulo_deberiaRetornarVacio() {
        var engine = new KnowledgeEngine(null);

        var result = engine.evaluate(KnowledgeInput.of(KnowledgeRequest.of("t", List.of())));

        assertTrue(result.isEmpty());
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var engine = new KnowledgeEngine(gatewayWith("src-1"));
        var input = KnowledgeInput.of(KnowledgeRequest.of("mercado", List.of("retail")));

        var r1 = engine.evaluate(input);
        var r2 = engine.evaluate(input);

        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.facts(), r2.facts());
        assertEquals(r1.explanation(), r2.explanation());
    }

    @Test
    void evaluate_deberiaConsolidarVariasFuentes() {
        var engine = new KnowledgeEngine(gatewayWith("src-1", "src-2"));

        var result = engine.evaluate(KnowledgeInput.of(KnowledgeRequest.of("t", List.of())));

        assertEquals(2, result.factCount());
        assertEquals(2, result.sourcesUsed().size());
    }
}
