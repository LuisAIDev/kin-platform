package com.kinplatform.ai.interview.adapter;

import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewState;

import java.util.Map;
import java.util.UUID;

/**
 * Mapeo puro entre el dominio ({@link InterviewState}) y el DTO de
 * persistencia ({@link InterviewStateData}) de la entrevista estratégica
 * (ADR-015). No contiene lógica de negocio ni depende de Spring.
 */
public final class InterviewStateMapper {

    public InterviewStateData toData(InterviewState state) {
        if (state == null) {
            return null;
        }
        return new InterviewStateData(
            state.answered(),
            state.pending(),
            state.current(),
            state.refinements(),
            state.complete(),
            state.exchangeBudget(),
            state.exchangeUsed()
        );
    }

    public InterviewState toDomain(UUID projectId, InterviewStateData data) {
        if (data == null || projectId == null) {
            return null;
        }
        return InterviewState.restore(
            projectId,
            data.answered() == null ? Map.<String, InterviewAnswer>of() : data.answered(),
            data.pending(),
            data.current(),
            data.refinements() == null ? Map.<String, Integer>of() : data.refinements(),
            data.complete(),
            data.exchangeBudget(),
            data.exchangeUsed()
        );
    }
}
