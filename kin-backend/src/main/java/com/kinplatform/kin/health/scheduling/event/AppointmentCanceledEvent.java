package com.kinplatform.kin.health.scheduling.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando una cita se cancela (paciente o médico), con motivo.
 */
public record AppointmentCanceledEvent(UUID appointmentId, UUID patientId, UUID physicianId, String reason)
        implements DomainEvent, HasUserId {

    public AppointmentCanceledEvent {
        reason = reason == null ? "" : reason;
    }

    @Override
    public String type() {
        return "appointment_canceled";
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

