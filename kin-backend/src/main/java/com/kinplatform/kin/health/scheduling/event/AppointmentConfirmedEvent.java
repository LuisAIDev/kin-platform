package com.kinplatform.kin.health.scheduling.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando el médico confirma una cita.
 */
public record AppointmentConfirmedEvent(UUID appointmentId, UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "appointment_confirmed";
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
