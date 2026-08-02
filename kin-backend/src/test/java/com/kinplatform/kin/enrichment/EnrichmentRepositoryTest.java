package com.kinplatform.kin.enrichment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrichmentRepositoryTest {

    private final UUID projectId = UUID.randomUUID();

    @Test
    void findOrEmpty_deberiaDevolverVacioSinResultadoPersistido() {
        EnrichmentRepository repo = new EnrichmentRepository() {
            @Override
            public Optional<EnrichmentResult> find(UUID id) {
                return Optional.empty();
            }

            @Override
            public void save(UUID id, EnrichmentResult result) {
            }
        };

        var result = repo.findOrEmpty(projectId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findOrEmpty_deberiaDevolverElResultadoPersistido() {
        var persisted = new EnrichmentResult(List.of(), List.of("src-1"), 0.5, "e", "E", "v1");
        EnrichmentRepository repo = new EnrichmentRepository() {
            @Override
            public Optional<EnrichmentResult> find(UUID id) {
                return Optional.of(persisted);
            }

            @Override
            public void save(UUID id, EnrichmentResult result) {
            }
        };

        var result = repo.findOrEmpty(projectId);

        assertEquals(persisted, result);
    }

    @Test
    void save_deberiaPersistirSinLanzar() {
        EnrichmentRepository repo = new EnrichmentRepository() {
            private EnrichmentResult saved;

            @Override
            public Optional<EnrichmentResult> find(UUID id) {
                return Optional.ofNullable(saved);
            }

            @Override
            public void save(UUID id, EnrichmentResult result) {
                this.saved = result;
            }
        };

        var result = new EnrichmentResult(List.of(), List.of(), 0.0, "", "E", "v1");
        repo.save(projectId, result);

        assertEquals(result, repo.find(projectId).orElseThrow());
    }
}
