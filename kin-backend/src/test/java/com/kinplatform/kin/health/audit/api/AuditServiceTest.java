package com.kinplatform.common.audit.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.common.audit.config.AuditProperties;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.common.audit.event.AuditLogEvent;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests del servicio de auditoría (ADR-035): publica el evento de forma
 * asíncrona y respeta el feature flag.
 */
class AuditServiceTest {

    private static final UUID USER = UUID.randomUUID();

    private AuditService service(boolean enabled, InMemoryDomainEventBus bus) {
        var props = new AuditProperties();
        props.setEnabled(enabled);
        return new AuditService(bus, null, null, props);
    }

    @Test
    void logAccess_deberiaPublicarEvento() {
        var bus = new InMemoryDomainEventBus();
        var service = service(true, bus);

        service.logAccess(USER, AuditAction.VIEW_HISTORY, AuditResourceType.PACIENTE,
                UUID.randomUUID(), UUID.randomUUID(), Map.of("key", "value"));

        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof AuditLogEvent));
        AuditLogEvent event = (AuditLogEvent) bus.publishedEvents().stream()
                .filter(e -> e instanceof AuditLogEvent)
                .findFirst()
                .orElseThrow();
        assertEquals(AuditAction.VIEW_HISTORY, event.action());
        assertEquals(AuditResourceType.PACIENTE, event.resourceType());
        assertEquals("value", event.details().get("key"));
    }

    @Test
    void logAccess_deshabilitado_deberiaNoPublicar() {
        var bus = new InMemoryDomainEventBus();
        var service = service(false, bus);

        service.logAccess(USER, AuditAction.VIEW_HISTORY, AuditResourceType.PACIENTE, null, null, null);

        assertTrue(bus.publishedEvents().isEmpty());
    }

    @Test
    void logAccess_conUserIdNulo_deberiaNoPublicar() {
        var bus = new InMemoryDomainEventBus();
        var service = service(true, bus);

        service.logAccess(null, AuditAction.VIEW_HISTORY, AuditResourceType.PACIENTE, null, null, null);

        assertTrue(bus.publishedEvents().isEmpty());
    }

    @Test
    void logAccess_nuncaDebeLanzar() {
        var bus = new InMemoryDomainEventBus();
        var service = service(true, bus);

        // No debe lanzar aunque el outbox falle (usa eventBus).
        service.logAccess(USER, AuditAction.SEND_MESSAGE, AuditResourceType.MENSAJE, null, null, null);
        assertTrue(true);
    }
}


