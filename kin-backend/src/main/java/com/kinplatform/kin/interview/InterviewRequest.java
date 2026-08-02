package com.kinplatform.kin.interview;

/**
 * Solicitud de dominio para procesar un turno de la entrevista (ADR-015).
 *
 * <p>Contiene la proyección del proyecto ({@link InterviewContext}), la
 * respuesta del usuario al turno ({@link InterviewAnswer}, {@code null} en el
 * primer turno) y el estado previo de la entrevista ({@link InterviewState}).
 * Es el input de dominio que consumirá {@code InterviewEngine} (Etapa E3).</p>
 */
public record InterviewRequest(
    InterviewContext context,
    InterviewAnswer answer,
    InterviewState previousState
) {

    public InterviewRequest {
        if (context == null) {
            throw new IllegalArgumentException("context no puede ser null");
        }
        if (previousState == null) {
            throw new IllegalArgumentException("previousState no puede ser null");
        }
    }

    public static InterviewRequest of(InterviewContext context, InterviewAnswer answer, InterviewState previousState) {
        return new InterviewRequest(context, answer, previousState);
    }

    public boolean hasAnswer() {
        return answer != null;
    }
}
