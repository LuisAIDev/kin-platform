package com.kinplatform.kin.health.followup.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando una tarea de seguimiento se completa (paciente o médico).
 */
public record FollowUpTaskCompletedEvent(UUID taskId, UUID planId, UUID patientId, UUID physicianId, UUID completedBy)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "followup_task_completed";
    }

    @Override
    public Object aggregateId() {
        return taskId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}
