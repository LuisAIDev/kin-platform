package com.kinplatform.kin.health.scheduling.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un paciente solicita una cita (estado PENDIENTE).
 */
public record AppointmentRequestedEvent(UUID appointmentId, UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "appointment_requested";
    }

    @Override
    public Object aggregateId() {
        return appointmentId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}
