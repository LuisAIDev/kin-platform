package com.kinplatform.kin.health.physician.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico invita a un paciente a vincularse.
 *
 * <p>Alimenta futuras notificaciones (correo, push) y registros de auditoría
 * sin acoplar el dominio de la relación al bus. La relación queda en
 * {@code PENDING} hasta que el paciente acepte o rechace.</p>
 */
public record PatientInvitedEvent(
        UUID patientId,
        UUID physicianId,
        String physicianName,
        String message)
        implements DomainEvent, HasUserId {

    public PatientInvitedEvent {
        physicianName = physicianName == null ? "" : physicianName;
        message = message == null ? "" : message;
    }

    @Override
    public String type() {
        return "patient_invited";
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
