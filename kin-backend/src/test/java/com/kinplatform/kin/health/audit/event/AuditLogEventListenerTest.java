package com.kinplatform.common.audit.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.common.audit.InMemoryAuditLogRepository;
import com.kinplatform.common.audit.config.AuditProperties;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests del listener que persiste los logs de auditoría (ADR-035).
 */
class AuditLogEventListenerTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    @Test
    void evento_deberiaGuardarLog() {
        var bus = new InMemoryDomainEventBus();
        var repo = new InMemoryAuditLogRepository();
        var props = new AuditProperties();
        var listener = new AuditLogEventListener(bus, repo, props);
        listener.register();

        bus.publish(new AuditLogEvent(USER, AuditAction.VIEW_SUMMARY, AuditResourceType.PACIENTE,
                PATIENT, PATIENT, "1.2.3.4", "test-agent", Map.of("x", 1)));

        assertEquals(1, repo.all().size());
        var log = repo.all().get(0);
        assertEquals(USER, log.userId());
        assertEquals(AuditAction.VIEW_SUMMARY, log.action());
        assertEquals(PATIENT, log.patientId());
        assertEquals("1.2.3.4", log.ipAddress());
        assertEquals("test-agent", log.userAgent());
        assertEquals(1, log.details().get("x"));
    }

    @Test
    void eventoConMóduloDeshabilitado_deberiaNoGuardar() {
        var bus = new InMemoryDomainEventBus();
        var repo = new InMemoryAuditLogRepository();
        var props = new AuditProperties();
        props.setEnabled(false);
        var listener = new AuditLogEventListener(bus, repo, props);
        listener.register();

        bus.publish(new AuditLogEvent(USER, AuditAction.VIEW_SUMMARY, AuditResourceType.PACIENTE,
                null, null, "", "", null));

        assertEquals(0, repo.all().size());
    }
}


