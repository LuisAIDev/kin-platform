package com.kinplatform.kin.health.followup.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico crea un plan de seguimiento para un paciente.
 */
public record FollowUpPlanCreatedEvent(UUID planId, UUID patientId, UUID physicianId)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "followup_plan_created";
    }

    @Override
    public Object aggregateId() {
        return planId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}
