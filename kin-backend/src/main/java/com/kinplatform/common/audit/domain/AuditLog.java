package com.kinplatform.common.audit.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Registro de auditoría de un acceso a datos de salud (ADR-035).
 *
 * <p>Inmutable: una vez guardado no se modifica. Captura quién ({@code userId}),
 * qué ({@code action}), a qué recurso ({@code resourceType}/{@code resourceId}),
 * sobre qué paciente (si aplica), cuándo, y contexto (IP, user-agent y
 * detalles opcionales en un {@code Map}).</p>
 */
public record AuditLog(
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

    public AuditLog {
        if (id == null || userId == null || action == null || resourceType == null) {
            throw new IllegalArgumentException("id/userId/action/resourceType no pueden ser null");
        }
        timestamp = timestamp == null ? OffsetDateTime.now() : timestamp;
        ipAddress = ipAddress == null ? "" : ipAddress;
        userAgent = userAgent == null ? "" : userAgent;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static AuditLog of(
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
        return new AuditLog(id, userId, action, resourceType, resourceId, patientId, timestamp, ipAddress, userAgent,
                details);
    }
}

