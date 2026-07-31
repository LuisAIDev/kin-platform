package com.kinplatform.kin.event;

import java.util.UUID;

public record ConversationCompletedEvent(
    UUID projectId,
    int totalExchanges,
    int dimensionsCovered,
    String finalDecision
) implements DomainEvent {
    @Override
    public String type() { return "conversation_completed"; }
    @Override
    public Object aggregateId() { return projectId; }
}
