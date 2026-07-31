package com.kinplatform.kin.event;

import java.util.UUID;

public record ScoreCalculatedEvent(
    UUID projectId,
    int totalScore,
    String viabilityLevel,
    int dimensionsCovered
) implements DomainEvent {
    @Override
    public String type() { return "score_calculated"; }
    @Override
    public Object aggregateId() { return projectId; }
}
