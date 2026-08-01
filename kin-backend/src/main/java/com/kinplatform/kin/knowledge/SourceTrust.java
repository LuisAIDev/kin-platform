package com.kinplatform.kin.knowledge;

/**
 * Nivel de confianza de una fuente de conocimiento externo (ADR-014).
 *
 * <p>Se deriva en Java a partir del tipo de origen (fuente pública oficial,
 * fuente secundaria o fuente no verificada). Es un valor del dominio: nunca se
 * le pregunta al LLM si una fuente es confiable.</p>
 */
public enum SourceTrust {

    OFFICIAL_PUBLIC("Fuente pública oficial"),
    SECONDARY("Fuente secundaria"),
    UNVERIFIED("Fuente no verificada");

    private final String displayName;

    SourceTrust(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
