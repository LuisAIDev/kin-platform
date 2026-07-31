package com.kinplatform.kin.event;

import java.util.UUID;

public record QuestionGeneratedEvent(
    UUID projectId,
    String dimension,
    String reason
) implements DomainEvent {
    @Override
    public String type() { return "question_generated"; }
    @Override
    public Object aggregateId() { return projectId; }
}
