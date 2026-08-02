package com.kinplatform.kin.interview;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación en memoria del puerto {@link InterviewRepository} para tests de
 * integración (Etapa E6): permite verificar la durabilidad del estado entre turnos
 * sin infraestructura.
 */
public class InMemoryInterviewRepository implements InterviewRepository {

    private InterviewState stored;

    public InMemoryInterviewRepository() {
        this(null);
    }

    public InMemoryInterviewRepository(InterviewState stored) {
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
