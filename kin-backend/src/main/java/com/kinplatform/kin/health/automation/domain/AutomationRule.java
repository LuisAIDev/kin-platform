package com.kinplatform.kin.health.automation.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AutomationRule(
        UUID id,
        String name,
        String description,
        TriggerEvent triggerEvent,
        String conditions,
        ActionType action,
        String actionParams,
        boolean enabled,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public AutomationRule {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name no puede ser vacío");
        }
        if (triggerEvent == null) {
            throw new IllegalArgumentException("triggerEvent no puede ser null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action no puede ser null");
        }
        if (conditions == null) {
            throw new IllegalArgumentException("conditions no puede ser null");
        }
        if (actionParams == null) {
            throw new IllegalArgumentException("actionParams no puede ser null");
        }
    }

    public static AutomationRule of(
            String name,
            String description,
            TriggerEvent triggerEvent,
            String conditions,
            ActionType action,
            String actionParams,
            boolean enabled,
            UUID createdBy) {
        var now = OffsetDateTime.now();
        return new AutomationRule(
                UUID.randomUUID(),
                name,
                description,
                triggerEvent,
                conditions,
                action,
                actionParams,
                enabled,
                createdBy,
                now,
                now);
    }
}