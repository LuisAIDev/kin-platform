package com.kinplatform.kin.health.triage.domain;

import java.util.UUID;

/**
 * Condición médica candidata del catálogo de triaje (ADR-028).
 *
 * <p>Entidad de dominio inmutable: describe una condición con su severidad,
 * urgencia de atención, una recomendación breve de apoyo (no sustituye el
 * diagnóstico médico) y el estado de validación clínica
 * ({@link ValidationStatus}, fase de consolidación ADR-028).</p>
 */
public record Condition(
        UUID id,
        String name,
        String description,
        String icdCode,
        Severity severity,
        Urgency urgency,
        String recommendation,
        ValidationStatus validationStatus) {

    public Condition {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name no puede ser null o vacío");
        }
        description = description == null ? "" : description;
        icdCode = icdCode == null ? "" : icdCode;
        severity = severity == null ? Severity.LEVE : severity;
        urgency = urgency == null ? Urgency.BAJA : urgency;
        recommendation = recommendation == null ? "" : recommendation;
        validationStatus = validationStatus == null ? ValidationStatus.PENDING : validationStatus;
    }

    /**
     * Constructor de compatibilidad (sin estado de validación): se asume
     * {@code PENDING}.
     */
    public Condition(
            UUID id,
            String name,
            String description,
            String icdCode,
            Severity severity,
            Urgency urgency,
            String recommendation) {
        this(id, name, description, icdCode, severity, urgency, recommendation, ValidationStatus.PENDING);
    }

    public static Condition of(
            UUID id,
            String name,
            String description,
            String icdCode,
            Severity severity,
            Urgency urgency,
            String recommendation) {
        return new Condition(
                id, name, description, icdCode, severity, urgency, recommendation, ValidationStatus.PENDING);
    }

    public static Condition of(
            UUID id,
            String name,
            String description,
            String icdCode,
            Severity severity,
            Urgency urgency,
            String recommendation,
            ValidationStatus validationStatus) {
        return new Condition(id, name, description, icdCode, severity, urgency, recommendation, validationStatus);
    }
}
