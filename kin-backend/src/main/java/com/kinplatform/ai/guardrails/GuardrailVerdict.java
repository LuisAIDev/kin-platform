package com.kinplatform.ai.guardrails;

import java.util.List;

/**
 * Veredicto inmutable de los guardrails del prompt (Fase 15 — capa de aplicación).
 */
public record GuardrailVerdict(
    GuardrailStatus status,
    List<String> reasons
) {

    public GuardrailVerdict {
        status = status == null ? GuardrailStatus.SAFE : status;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public boolean safe() {
        return status == GuardrailStatus.SAFE;
    }

    public boolean blocked() {
        return status == GuardrailStatus.BLOCKED;
    }

    public boolean flagged() {
        return status == GuardrailStatus.FLAGGED;
    }

    public static GuardrailVerdict allowed() {
        return new GuardrailVerdict(GuardrailStatus.SAFE, List.of());
    }

    public static GuardrailVerdict of(GuardrailStatus status, String reason) {
        return new GuardrailVerdict(status, List.of(reason == null ? "" : reason));
    }
}
