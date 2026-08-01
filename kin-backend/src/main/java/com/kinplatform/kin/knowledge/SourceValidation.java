package com.kinplatform.kin.knowledge;

import java.util.List;

/**
 * Resultado determinista de la validación de una fuente (ADR-014): aceptada o
 * rechazada, con los motivos y el nivel de confianza derivado en Java.
 *
 * <p>Las reglas de validación (protocolo, allowlist, frescura, formato,
 * deduplicación) las aplica el validador de fuentes; este tipo solo transporta
 * el resultado. Nunca participa el LLM.</p>
 */
public record SourceValidation(
    boolean accepted,
    SourceTrust trust,
    List<String> reasons
) {

    public SourceValidation {
        trust = trust == null ? SourceTrust.UNVERIFIED : trust;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static SourceValidation accepted(SourceTrust trust) {
        return new SourceValidation(true, trust, List.of());
    }

    public static SourceValidation rejected(String reason) {
        return new SourceValidation(false, SourceTrust.UNVERIFIED, List.of(reason == null ? "" : reason));
    }
}
