package com.kinplatform.ai.interview.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewRepository;
import com.kinplatform.kin.interview.InterviewState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaInterviewRepositoryTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private InterviewStateJpaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JpaInterviewRepository interviewRepository;

    private JpaInterviewRepository repo() {
        if (interviewRepository == null) {
            interviewRepository = new JpaInterviewRepository(repository, objectMapper);
        }
        return interviewRepository;
    }

    @Test
    void find_deberiaRestaurarElEstadoPersistido() {
        var original = InterviewState.empty(PROJECT_ID)
            .withAnswered(Map.of("q-proyecto", InterviewAnswer.of("q-proyecto", "Mi App")))
            .withPending(List.of("q-sector"))
            .withCurrent("q-sector")
            .withComplete(false)
            .withExchangeUsed(1);

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repo().save(original);
        var captor = ArgumentCaptor.forClass(InterviewStateEntity.class);
        verify(repository).save(captor.capture());
        clearInvocations(repository);

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.of(captor.getValue()));
        var restored = repo().find(PROJECT_ID).orElseThrow();

        assertEquals(PROJECT_ID, restored.projectId());
        assertEquals(original.answered(), restored.answered());
        assertEquals(original.pending(), restored.pending());
        assertEquals("q-sector", restored.current());
        assertEquals(1, restored.exchangeUsed());
        assertEquals(InterviewState.DEFAULT_EXCHANGE_BUDGET, restored.exchangeBudget());
    }

    @Test
    void find_sinEstado_deberiaDevolverVacio() {
        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertTrue(repo().find(PROJECT_ID).isEmpty());
    }

    @Test
    void find_conProjectIdNull_deberiaDevolverVacio() {
        assertTrue(repo().find(null).isEmpty());
    }

    @Test
    void save_deberiaPersistirEstadoNuevo() {
        var state = InterviewState.empty(PROJECT_ID);

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repo().save(state);

        var captor = ArgumentCaptor.forClass(InterviewStateEntity.class);
        verify(repository).save(captor.capture());
        var entity = captor.getValue();
        assertEquals(PROJECT_ID, entity.getProjectId());
        assertNotNull(entity.getStateData());
        assertTrue(entity.getStateData().contains("exchangeBudget"));
    }

    @Test
    void save_deberiaActualizarEstadoExistente() {
        var existing = InterviewStateEntity.builder()
            .projectId(PROJECT_ID)
            .stateData("{}")
            .updatedAt(java.time.OffsetDateTime.now())
            .build();
        when(repository.findById(PROJECT_ID)).thenReturn(Optional.of(existing));

        var updated = InterviewState.empty(PROJECT_ID).withComplete(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repo().save(updated);

        var captor = ArgumentCaptor.forClass(InterviewStateEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(PROJECT_ID, captor.getValue().getProjectId());
        assertTrue(captor.getValue().getStateData().contains("\"complete\":true"));
    }

    @Test
    void save_conStateNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> repo().save(null));
        verify(repository, never()).save(any());
    }

    @Test
    void findOrCreate_sinEstado_deberiaCrearEstadoVacio() {
        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        var state = repo().findOrCreate(PROJECT_ID);

        assertEquals(PROJECT_ID, state.projectId());
        assertTrue(state.answered().isEmpty());
        assertEquals(InterviewState.DEFAULT_EXCHANGE_BUDGET, state.exchangeBudget());
    }

    @Test
    void findOrCreate_conEstadoPersistido_deberiaDevolverlo() {
        var persisted = InterviewState.empty(PROJECT_ID).withComplete(true);
        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repo().save(persisted);
        var captor = ArgumentCaptor.forClass(InterviewStateEntity.class);
        verify(repository).save(captor.capture());
        clearInvocations(repository);

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.of(captor.getValue()));
        var state = repo().findOrCreate(PROJECT_ID);

        assertEquals(PROJECT_ID, state.projectId());
        assertTrue(state.complete());
    }

    @Test
    void save_conErrorDeSerializacion_deberiaLanzarIllegalState() throws JsonProcessingException {
        var failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
            }
        };
        var repoWithError = new JpaInterviewRepository(repository, failingMapper);

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> repoWithError.save(InterviewState.empty(PROJECT_ID)));
    }

    @Test
    void find_conJsonInvalido_deberiaLanzarIllegalState() {
        var entity = InterviewStateEntity.builder()
            .projectId(PROJECT_ID)
            .stateData("not-json")
            .build();

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));

        assertThrows(IllegalStateException.class, () -> repo().find(PROJECT_ID));
    }

    @Test
    void contrato_deberiaSerInterviewRepository() {
        assertTrue(InterviewRepository.class.isAssignableFrom(JpaInterviewRepository.class));
        assertFalse(JpaInterviewRepository.class.isInterface());
    }
}
