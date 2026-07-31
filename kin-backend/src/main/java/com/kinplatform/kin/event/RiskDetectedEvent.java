package com.kinplatform.kin.event;

import java.util.UUID;

public record RiskDetectedEvent(
    UUID projectId,
    String risk,
    String severity
) implements DomainEvent {
    @Override
    public String type() { return "risk_detected"; }
    @Override
    public Object aggregateId() { return projectId; }
}
