package com.kinplatform.kin.health.automation.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RuleExecutionLog(
        UUID id,
        UUID ruleId,
        UUID eventId,
        OffsetDateTime triggeredAt,
        boolean executed,
        String error,
        String details) {

    public RuleExecutionLog {
        if (ruleId == null) {
            throw new IllegalArgumentException("ruleId no puede ser null");
        }
        if (eventId == null) {
            throw new IllegalArgumentException("eventId no puede ser null");
        }
        if (triggeredAt == null) {
            throw new IllegalArgumentException("triggeredAt no puede ser null");
        }
    }

    public static RuleExecutionLog of(UUID ruleId, UUID eventId) {
        var now = OffsetDateTime.now();
        return new RuleExecutionLog(
                UUID.randomUUID(),
                ruleId,
                eventId,
                now,
                false,
                null,
                null);
    }
}