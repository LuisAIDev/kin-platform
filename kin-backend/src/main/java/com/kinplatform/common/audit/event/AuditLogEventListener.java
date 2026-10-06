package com.kinplatform.common.audit.event;

import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.common.audit.config.AuditProperties;
import com.kinplatform.common.audit.domain.AuditLog;
import com.kinplatform.common.audit.port.AuditLogRepository;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Listener que persiste los eventos de auditoría (ADR-035).
 *
 * <p>Se suscribe al {@link DomainEventBus} y guarda el {@link AuditLog} de forma
 * determinista. La entrega es asíncrona respecto a la operación principal (el
 * evento viaja por el outbox y el relay lo despacha en su hilo de polling).</p>
 */
@Component
public class AuditLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogEventListener.class);

    private final DomainEventBus eventBus;
    private final AuditLogRepository auditLogRepository;
    private final AuditProperties properties;

    public AuditLogEventListener(
            DomainEventBus eventBus, AuditLogRepository auditLogRepository, AuditProperties properties) {
        this.eventBus = eventBus;
        this.auditLogRepository = auditLogRepository;
        this.properties = properties;
    }

    @PostConstruct
    public void register() {
        eventBus.subscribe(AuditLogEvent.class, this::onAuditLogEvent);
    }

    void onAuditLogEvent(AuditLogEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        if (!properties.isEnabled()) {
            return;
        }
        try {
            AuditLog auditLog = AuditLog.of(
                    UUID.randomUUID(),
                    event.userId(),
                    event.action(),
                    event.resourceType(),
                    event.resourceId(),
                    event.patientId(),
                    OffsetDateTime.now(),
                    event.ipAddress(),
                    event.userAgent(),
                    event.details());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // La auditoría no debe romper el flujo principal: se registra y continúa.
            log.error("AuditLogEventListener: fallo al guardar log de auditoría: {}", e.getMessage(), e);
        }
    }
}


