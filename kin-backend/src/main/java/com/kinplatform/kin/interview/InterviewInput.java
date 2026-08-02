package com.kinplatform.kin.interview;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada canonizada del motor de entrevista (ADR-015, ADR-005/009).
 *
 * <p>Envuelve la {@link InterviewRequest} del turno y el mensaje crudo del
 * usuario (necesario para la validación de respuestas). Implementa
 * {@link EngineInput} para que {@code InterviewEngine} (Etapa E3) sea ejecutable
 * por la infraestructura común de motores.</p>
 */
public record InterviewInput(
    InterviewRequest request,
    String userMessage
) implements EngineInput {

    public InterviewInput {
        if (request == null) {
            throw new IllegalArgumentException("request no puede ser null");
        }
        userMessage = userMessage == null ? "" : userMessage;
    }

    public static InterviewInput of(InterviewRequest request, String userMessage) {
        return new InterviewInput(request, userMessage);
    }
}
