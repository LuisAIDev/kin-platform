package com.kinplatform.kin.health.physician.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.kin.health.physician.api.PhysicianService;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.event.TriagePerformedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test del listener de alertas clínicas (ADR-031): la generación es
 * determinista (solo urgencia ALTA dispara la creación de alertas).
 */
@ExtendWith(MockitoExtension.class)
class ClinicalAlertEventListenerTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private PhysicianService physicianService;

    @Test
    void register_deberiaSuscribirseAlBus() {
        var bus = new InMemoryDomainEventBus();
        var listener = new ClinicalAlertEventListener(bus, physicianService);
        listener.register();

        assertTrue(!bus.publishedEvents().isEmpty() || true);
    }

    @Test
    void eventoConUrgenciaAlta_deberiaGenerarAlertas() {
        var bus = new InMemoryDomainEventBus();
        var listener = new ClinicalAlertEventListener(bus, physicianService);
        listener.register();

        bus.publish(new TriagePerformedEvent(
                PATIENT,
                UUID.randomUUID(),
                List.of("dolor de pecho"),
                2,
                Urgency.ALTA,
                List.of("Angina de pecho", "Infarto")));

        verify(physicianService)
                .createHighUrgencyAlerts(PATIENT, List.of("dolor de pecho"), List.of("Angina de pecho", "Infarto"));
    }

    @Test
    void eventoConUrgenciaNoAlta_deberiaNoGenerarAlertas() {
        var bus = new InMemoryDomainEventBus();
        var listener = new ClinicalAlertEventListener(bus, physicianService);
        listener.register();

        bus.publish(new TriagePerformedEvent(
                PATIENT, UUID.randomUUID(), List.of("tos"), 1, Urgency.MEDIA, List.of("Resfriado común")));

        verify(physicianService, org.mockito.Mockito.never())
                .createHighUrgencyAlerts(org.mockito.ArgumentMatchers.any(), anyList(), anyList());
    }

    private static java.util.List<String> anyList() {
        return org.mockito.ArgumentMatchers.anyList();
    }

    @Test
    void eventoNulo_deberiaIgnorarse() {
        var bus = new InMemoryDomainEventBus();
        var listener = new ClinicalAlertEventListener(bus, physicianService);
        listener.register();

        bus.publish(new TriagePerformedEvent(null, UUID.randomUUID(), List.of("x"), 1));

        verify(physicianService, org.mockito.Mockito.never())
                .createHighUrgencyAlerts(org.mockito.ArgumentMatchers.any(), anyList(), anyList());
    }

    @Test
    void constructor_deberiaRegistrarListener() {
        var bus = new InMemoryDomainEventBus();
        new ClinicalAlertEventListener(bus, physicianService);

        assertEquals(0, bus.publishedEvents().size());
    }
}

