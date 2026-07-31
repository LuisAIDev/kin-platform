package com.kinplatform.kin.event;

import java.util.UUID;

public record ReportGeneratedEvent(
    UUID projectId,
    String format
) implements DomainEvent {
    @Override
    public String type() { return "report_generated"; }
    @Override
    public Object aggregateId() { return projectId; }
}
