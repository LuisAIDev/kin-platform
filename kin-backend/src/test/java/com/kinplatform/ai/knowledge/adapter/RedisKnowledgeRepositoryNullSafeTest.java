package com.kinplatform.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Comportamiento seguro del adaptador Redis con caché ausente o inválida
 * (sin servidor): nunca lanza, degrada a {@code empty} y no registra métricas
 * falsas de acierto.
 */
class RedisKnowledgeRepositoryNullSafeTest {

    private final KnowledgeAdapterMetrics metrics =
            new KnowledgeAdapterMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    private final RedisKnowledgeRepository repository = new RedisKnowledgeRepository(null, new ObjectMapper(), metrics);

    private static KnowledgeQuery query(String topic) {
        return KnowledgeQuery.from(KnowledgeRequest.of(topic, java.util.List.of()));
    }

    @Test
    void sinRedis_find_deberiaDevolverVacioSinLanzar() {
        Optional<KnowledgeResult> result = repository.find(query("retail"));

        assertFalse(result.isPresent());
        assertEquals(0.0, metrics.count("kin.knowledge.adapter.cache.hit"));
    }

    @Test
    void sinRedis_save_noDeberiaLanzar() {
        KnowledgeResult result = KnowledgeResult.empty();

        repository.save(query("retail"), result, Duration.ofHours(1));
        repository.save(result, Duration.ofHours(1));
        repository.save(null, Duration.ofHours(1));
        repository.save(null, null, null);

        assertTrue(true);
    }

    @Test
    void claves_deberianSerDeterministas() {
        String keyA = RedisKnowledgeRepository.queryKey(query("retail"));
        String keyB = RedisKnowledgeRepository.queryKey(query("retail"));
        String keyC = RedisKnowledgeRepository.queryKey(query("mercado"));

        assertEquals(keyA, keyB);
        assertFalse(keyA.equals(keyC));
    }
}
