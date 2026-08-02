package com.kinplatform.kin.conversation;

import java.util.List;

/**
 * Fallback determinista de respuesta ante una comunicación no aceptada
 * (ADR-017, Etapa E2).
 *
 * <p>Cuando {@link ResponseValidation#accepted()} es {@code false}, Java decide
 * la respuesta enlatada (español) y si conviene reintentar, con un reintento
 * acotado. Nunca infiere intención del texto del LLM (misma garantía que
 * {@link ResponseGuard}). Clase final inmutable y sin infraestructura.</p>
 */
public final class ResponseFallback {

    public static final String DEFAULT_CANNED_RESPONSE =
        "No pude generar una respuesta válida en este momento. Por favor, intenta de nuevo.";

    private final List<String> cannedResponses;
    private final int maxRetries;

    public ResponseFallback() {
        this(List.of(DEFAULT_CANNED_RESPONSE), 1);
    }

    /**
     * @param cannedResponses respuestas enlatadas (español); vacío → la por defecto
     * @param maxRetries      reintentos máximos permitidos ante {@code accepted=false}
     */
    public ResponseFallback(List<String> cannedResponses, int maxRetries) {
        this.cannedResponses = (cannedResponses == null || cannedResponses.isEmpty())
            ? List.of(DEFAULT_CANNED_RESPONSE)
            : List.copyOf(cannedResponses);
        this.maxRetries = Math.max(0, maxRetries);
    }

    public List<String> cannedResponses() {
        return cannedResponses;
    }

    public int maxRetries() {
        return maxRetries;
    }

    /**
     * Respuesta enlatada por defecto (la primera de la lista).
     */
    public String cannedResponse() {
        return cannedResponses.get(0);
    }

    /**
     * Respuesta enlatada contextual: incluye el primer issue de la validación
     * cuando existe; si no, la respuesta por defecto.
     */
    public String cannedResponse(ResponseValidation validation) {
        if (validation != null && validation.issues() != null && !validation.issues().isEmpty()) {
            return DEFAULT_CANNED_RESPONSE + " Motivo: " + validation.issues().get(0) + ".";
        }
        return cannedResponse();
    }

    /**
     * {@code true} si, ante una validación rechazada, todavía quedan reintentos
     * (intentos 1-based): se reintenta mientras {@code attempt <= maxRetries}
     * (el intento inicial más los {@code maxRetries} reintentos).
     */
    public boolean shouldRetry(ResponseValidation validation, int attempt) {
        return validation != null
            && !validation.accepted()
            && attempt > 0
            && attempt <= maxRetries;
    }
}
