package com.kinplatform.kin.health.documents.domain;

/**
 * Estado del ciclo de vida de un documento clínico (ADR-036).
 */
public enum DocumentStatus {
    ACTIVE,
    ARCHIVED,
    HIDDEN_FROM_PATIENT,
    DELETED
}
