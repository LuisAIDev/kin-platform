package com.kinplatform.kin.health.physician.config;

import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.kin.health.physician.api.PhysicianService;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.event.TriagePerformedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Listener de dominio que genera alertas clínicas deterministas (ADR-031).
 *
 * <p>Escucha {@link TriagePerformedEvent} y, cuando la urgencia máxima del
 * triaje es {@code ALTA}, crea una alerta para cada médico asignado al
 * paciente. Java decide la generación (regla determinista); el LLM nunca
 * participa. La persistencia la realiza {@link PhysicianService}.</p>
 */
@Component
public class ClinicalAlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(ClinicalAlertEventListener.class);

    private final DomainEventBus eventBus;
    private final PhysicianService physicianService;

    public ClinicalAlertEventListener(DomainEventBus eventBus, PhysicianService physicianService) {
        this.eventBus = eventBus;
        this.physicianService = physicianService;
    }

    @PostConstruct
    public void register() {
        eventBus.subscribe(TriagePerformedEvent.class, this::onTriagePerformed);
        log.info("ClinicalAlertEventListener registrado en el bus de eventos");
    }

    void onTriagePerformed(TriagePerformedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        if (event.maxUrgency() == Urgency.ALTA) {
            log.info("ClinicalAlertEventListener: triaje ALTA del paciente {} → generando alertas", event.userId());
            physicianService.createHighUrgencyAlerts(event.userId(), event.symptoms(), event.conditionNames());
        }
    }
}

