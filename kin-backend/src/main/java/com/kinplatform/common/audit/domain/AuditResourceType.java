package com.kinplatform.common.audit.domain;

/**
 * Tipos de recurso de salud accedidos (ADR-035).
 */
public enum AuditResourceType {
    PACIENTE,
    MENSAJE,
    CITA,
    PLAN_SEGUIMIENTO,
    TAREA,
    EVOLUCION,
    ALERTA,
    DOCUMENTO,
    AUTOMATION_RULE,
    AUTOMATION_RULE_EXECUTION,
    /** Usuario afectado (p. ej. decisión ADMIN sobre una solicitud profesional). */
    USER,
    /** Prescripción MIPRES. */
    MIPRES_PRESCRIPTION,
    /** Suministro MIPRES. */
    MIPRES_SUPPLY
}

