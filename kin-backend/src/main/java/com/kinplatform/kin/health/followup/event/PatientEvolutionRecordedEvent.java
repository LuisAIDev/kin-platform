package com.kinplatform.kin.health.followup.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico registra la evolución de un paciente.
 */
public record PatientEvolutionRecordedEvent(UUID evolutionId, UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "patient_evolution_recorded";
    }

    @Override
    public Object aggregateId() {
        return evolutionId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}
