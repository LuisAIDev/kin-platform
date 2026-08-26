package com.kinplatform.kin.health.dashboard.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import java.util.List;

/**
 * Mapeo puro entre el dominio del dashboard y las entidades JPA (ADR-030).
 * No contiene lógica de negocio ni depende de Spring.
 */
public final class DashboardMapper {

    private DashboardMapper() {}

    /** DTO serializado en {@code profile_data} (JSONB). */
    public record ProfileData(List<String> riskFactors, List<String> chronicConditions) {
        public ProfileData {
            riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
            chronicConditions = chronicConditions == null ? List.of() : List.copyOf(chronicConditions);
        }
    }

    public static ProfileData toData(PatientProfile profile) {
        return new ProfileData(profile.riskFactors(), profile.chronicConditions());
    }

    public static PatientProfile toProfile(
            java.util.UUID userId, ProfileData data, java.time.OffsetDateTime updatedAt, ObjectMapper mapper) {
        if (data == null) {
            return PatientProfile.empty(userId);
        }
        return PatientProfile.of(
                userId,
                data.riskFactors(),
                data.chronicConditions(),
                updatedAt == null ? java.time.OffsetDateTime.now() : updatedAt);
    }

    public static Reminder toDomain(ReminderEntity entity) {
        if (entity == null) {
            return null;
        }
        return Reminder.of(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getScheduledAt(),
                Boolean.TRUE.equals(entity.getActive()),
                entity.getCreatedAt());
    }

    public static ReminderEntity toEntity(Reminder reminder) {
        ReminderEntity entity = new ReminderEntity();
        entity.setId(reminder.id());
        entity.setUserId(reminder.userId());
        entity.setType(reminder.type());
        entity.setTitle(reminder.title());
        entity.setScheduledAt(reminder.scheduledAt());
        entity.setActive(reminder.active());
        entity.setCreatedAt(reminder.createdAt());
        return entity;
    }

    public static String toJson(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize dashboard profile", e);
        }
    }

    public static ProfileData fromJson(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return new ProfileData(List.of(), List.of());
        }
        try {
            return mapper.readValue(json, ProfileData.class);
        } catch (JsonProcessingException e) {
            return new ProfileData(List.of(), List.of());
        }
    }
}
