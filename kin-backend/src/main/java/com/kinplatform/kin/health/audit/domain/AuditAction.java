package com.kinplatform.kin.health.audit.domain;

/**
 * Acciones de acceso a datos de salud registradas en la auditoría (ADR-035).
 */
public enum AuditAction {
    VIEW_SUMMARY,
    VIEW_HISTORY,
    SEND_MESSAGE,
    READ_MESSAGES,
    REQUEST_APPOINTMENT,
    CONFIRM_APPOINTMENT,
    CANCEL_APPOINTMENT,
    RESCHEDULE_APPOINTMENT,
    COMPLETE_APPOINTMENT,
    CREATE_PLAN,
    ADD_TASK,
    COMPLETE_TASK,
    RECORD_EVOLUTION,
    VIEW_EVOLUTION,
    ACKNOWLEDGE_ALERT,
    UPLOAD_DOCUMENT,
    DOWNLOAD_DOCUMENT,
    DELETE_DOCUMENT,
    CREATE,
    UPDATE,
    TOGGLE,
    DELETE,
    EXECUTE
}
