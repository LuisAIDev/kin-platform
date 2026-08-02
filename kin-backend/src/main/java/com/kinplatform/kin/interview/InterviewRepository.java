package com.kinplatform.kin.interview;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del estado de la entrevista (ADR-015).
 *
 * <p>Definido en el dominio; la infraestructura lo implementará (Etapa E4) con
 * un almacén propio ({@code interview_state}), sin tocar {@code project_context}.
 * {@code findOrCreate} es un default determinista sobre {@code find}: devuelve
 * el estado persistido o un {@link InterviewState} vacío para el proyecto.</p>
 */
public interface InterviewRepository {

    Optional<InterviewState> find(UUID projectId);

    void save(InterviewState state);

    default InterviewState findOrCreate(UUID projectId) {
        return find(projectId).orElseGet(() -> InterviewState.empty(projectId));
    }
}
