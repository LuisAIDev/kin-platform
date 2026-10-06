package com.kinplatform.common.audit.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de auditoría publicado de forma transaccional (outbox) para guardar el
 * registro de forma asíncrona sin bloquear la operación principal (ADR-035).
 */
public record AuditLogEvent(
        UUID userId,
        AuditAction action,
        AuditResourceType resourceType,
        UUID resourceId,
        UUID patientId,
        String ipAddress,
        String userAgent,
        Map<String, Object> details) implements DomainEvent, HasUserId {

    public AuditLogEvent {
        ipAddress = ipAddress == null ? "" : ipAddress;
        userAgent = userAgent == null ? "" : userAgent;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    @Override
    public String type() {
        return "audit_log";
    }

    @Override
    public Object aggregateId() {
        return patientId != null ? patientId : resourceId != null ? resourceId : userId;
    }

    @Override
    public UUID userId() {
        return userId;
    }
}



