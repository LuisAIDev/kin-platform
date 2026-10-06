package com.kinplatform.common.audit.api;

import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditLog;
import com.kinplatform.common.audit.domain.AuditResourceType;
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


