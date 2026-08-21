package com.kinplatform.kin.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Cubre el contrato aditivo de ADR-021: el método <em>default</em>
 * {@link KnowledgeRepository#save(KnowledgeQuery, KnowledgeResult, Duration)}
 * delega en el contrato congelado {@link KnowledgeRepository#save(KnowledgeResult, Duration)}
 * para que los adaptadores existentes no cambien su comportamiento.
 */
class KnowledgeRepositoryDefaultSaveTest {

    private static final class RecordingRepository implements KnowledgeRepository {
        private final List<KnowledgeResult> saved = new ArrayList<>();

        @Override
        public Optional<KnowledgeResult> find(KnowledgeQuery query) {
            return Optional.empty();
        }

        @Override
        public void save(KnowledgeResult result, Duration ttl) {
            saved.add(result);
        }
    }

    @Test
    void defaultSave_deberiaDelegarEnSaveSinConsulta() {
        RecordingRepository repository = new RecordingRepository();
        KnowledgeResult result = KnowledgeResult.empty();
        KnowledgeQuery query = KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of()));

        repository.save(query, result, Duration.ofHours(1));

        assertEquals(1, repository.saved.size());
        assertSame(result, repository.saved.get(0));
    }
}
