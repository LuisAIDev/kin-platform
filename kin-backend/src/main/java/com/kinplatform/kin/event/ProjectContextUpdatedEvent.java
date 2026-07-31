package com.kinplatform.kin.event;

import java.util.Set;
import java.util.UUID;

public record ProjectContextUpdatedEvent(
    UUID projectId,
    Set<String> updatedDimensions,
    int knownDimensionsCount,
    int totalDimensions
) implements DomainEvent {
    @Override
    public String type() { return "project_context_updated"; }
    @Override
    public Object aggregateId() { return projectId; }
}
