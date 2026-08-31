package com.kinplatform.kin.health.audit.api;

import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditLog;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Registro de auditoría expuesto en la API (ADR-035).
 */
public record AuditLogResponse(
        UUID id,
        UUID userId,
        AuditAction action,
        AuditResourceType resourceType,
        UUID resourceId,
        UUID patientId,
        OffsetDateTime timestamp,
        String ipAddress,
        String userAgent,
        Map<String, Object> details) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.id(),
                log.userId(),
                log.action(),
                log.resourceType(),
                log.resourceId(),
                log.patientId(),
                log.timestamp(),
                log.ipAddress(),
                log.userAgent(),
                log.details());
    }
}
