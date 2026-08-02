package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewRepositoryTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static final class InMemoryInterviewRepository implements InterviewRepository {

        private InterviewState stored;

        InMemoryInterviewRepository(InterviewState stored) {
            this.stored = stored;
        }

        @Override
        public Optional<InterviewState> find(UUID projectId) {
            if (stored == null || !stored.projectId().equals(projectId)) {
                return Optional.empty();
            }
            return Optional.of(stored);
        }

        @Override
        public void save(InterviewState state) {
            this.stored = state;
        }
    }

    @Test
    void findOrCreate_deberiaDevolverEstadoPersistido() {
        var persisted = InterviewState.empty(PROJECT_ID).withComplete(true);
        var repository = new InMemoryInterviewRepository(persisted);

        var state = repository.findOrCreate(PROJECT_ID);

        assertSame(persisted, state);
        assertTrue(state.complete());
    }

    @Test
    void findOrCreate_sinEstado_deberiaCrearEstadoVacio() {
        var repository = new InMemoryInterviewRepository(null);

        var state = repository.findOrCreate(PROJECT_ID);

        assertEquals(PROJECT_ID, state.projectId());
        assertFalse(state.complete());
        assertEquals(0, state.answeredCount());
        assertEquals(InterviewState.DEFAULT_EXCHANGE_BUDGET, state.exchangeBudget());
    }

    @Test
    void findOrCreate_conOtroProyecto_deberiaCrearEstadoVacio() {
        var repository = new InMemoryInterviewRepository(InterviewState.empty(UUID.randomUUID()));

        var state = repository.findOrCreate(PROJECT_ID);

        assertEquals(PROJECT_ID, state.projectId());
        assertTrue(state.answered().isEmpty());
    }

    @Test
    void find_deberiaDevolverElEstadoAlmacenado() {
        var persisted = InterviewState.empty(PROJECT_ID);
        var repository = new InMemoryInterviewRepository(persisted);

        var found = repository.find(PROJECT_ID);

        assertTrue(found.isPresent());
        assertEquals(persisted, found.get());
    }

    @Test
    void find_sinEstado_deberiaDevolverVacio() {
        var repository = new InMemoryInterviewRepository(null);

        assertTrue(repository.find(PROJECT_ID).isEmpty());
    }

    @Test
    void save_deberiaPersistirElEstadoParaReconstruirlo() {
        var repository = new InMemoryInterviewRepository(null);
        var estadoGuardado = InterviewState.empty(PROJECT_ID)
            .withAnswered(java.util.Map.of("q1", InterviewAnswer.of("q1", "respuesta")));

        repository.save(estadoGuardado);
        var reconstruct = repository.findOrCreate(PROJECT_ID);

        assertEquals(estadoGuardado, reconstruct);
        assertTrue(reconstruct.hasAnswered("q1"));
    }

    @Test
    void contrato_deberiaExponerFindSaveYFindOrCreate() {
        var methods = java.util.Arrays.stream(InterviewRepository.class.getDeclaredMethods())
            .map(java.lang.reflect.Method::getName)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(InterviewRepository.class.isInterface());
        assertTrue(methods.contains("find"));
        assertTrue(methods.contains("save"));
        assertTrue(methods.contains("findOrCreate"));
    }
}
