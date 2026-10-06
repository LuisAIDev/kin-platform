package com.kinplatform.kin.health.physician.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico invita a un paciente a vincularse (o reenvía
 * la invitación a un paciente que ya tiene una {@code PENDING}/
 * {@code PENDING_CONSENT}).
 *
 * <p>Alimenta el envío del correo de invitación (o recordatorio de invitación
 * pendiente) sin acoplar el dominio de la relación al bus. La relación queda en
 * {@code PENDING} hasta que el paciente acepte o rechace.</p>
 */
public record PatientInvitedEvent(
        UUID patientId, UUID physicianId, String physicianName, String message, boolean resend)
        implements DomainEvent, HasUserId {

    public PatientInvitedEvent {
        physicianName = physicianName == null ? "" : physicianName;
        message = message == null ? "" : message;
    }

    /** Conveniencia: invitación inicial (no es un reenvío). */
    public PatientInvitedEvent(UUID patientId, UUID physicianId, String physicianName, String message) {
        this(patientId, physicianId, physicianName, message, false);
    }

    @Override
    public String type() {
        return resend ? "patient_invited_resend" : "patient_invited";
    }

    @Override
    public Object aggregateId() {
        return patientId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}

