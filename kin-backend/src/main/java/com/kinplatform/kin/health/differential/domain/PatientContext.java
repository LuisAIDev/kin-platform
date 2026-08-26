package com.kinplatform.kin.health.differential.domain;

import java.util.Set;

/**
 * Contexto del paciente para el diagnóstico diferencial (ADR-029).
 *
 * <p>Conjunto de características del paciente (p. ej. {@code fumador},
 * {@code embarazo}, {@code edad avanzada}) que el {@code DifferentialEngine}
 * usa para ajustar las probabilidades por factores de riesgo. Inmutable y
 * determinista; Java decide la coincidencia, nunca el LLM.</p>
 */
public record PatientContext(Set<String> riskFactors) {

    public PatientContext {
        riskFactors = riskFactors == null ? Set.of() : Set.copyOf(riskFactors);
    }

    public static PatientContext empty() {
        return new PatientContext(Set.of());
    }

    public static PatientContext of(String... factors) {
        return new PatientContext(Set.of(factors == null ? new String[0] : factors));
    }

    public static PatientContext of(Set<String> factors) {
        return new PatientContext(factors);
    }

    public boolean has(String factor) {
        if (factor == null || factor.isBlank()) {
            return false;
        }
        return riskFactors.stream().anyMatch(f -> f != null && f.equalsIgnoreCase(factor));
    }
}
