package com.kinplatform.kin.health.scheduling.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando una cita se reprograma: la original pasa a
 * {@code REPROGRAMADA} y se crea {@code newAppointmentId} con referencia a la
 * original en {@code rescheduledFrom}.
 */
public record AppointmentRescheduledEvent(
        UUID oldAppointmentId, UUID newAppointmentId, UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "appointment_rescheduled";
    }

    @Override
    public Object aggregateId() {
        return newAppointmentId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}

