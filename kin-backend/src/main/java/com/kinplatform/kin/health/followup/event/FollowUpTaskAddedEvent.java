package com.kinplatform.kin.health.followup.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico añade una tarea a un plan de seguimiento.
 */
public record FollowUpTaskAddedEvent(UUID taskId, UUID planId, UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "followup_task_added";
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
