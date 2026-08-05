package com.kinplatform.ai.guardrails;

/**
 * Resultado de la capa de guardrails del prompt (Fase 15 — capa de aplicación).
 *
 * <p>{@code SAFE}: se puede procesar. {@code FLAGGED}: contiene señales menores
 * (se procesa pero se registra). {@code BLOCKED}: intento de inyección/jailbreak
 * detectado (no se envía al LLM). Determinista, sin LLM.</p>
 */
public enum GuardrailStatus {

    SAFE("Seguro"),
    FLAGGED("Marcado"),
    BLOCKED("Bloqueado");

    private final String displayName;

    GuardrailStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
