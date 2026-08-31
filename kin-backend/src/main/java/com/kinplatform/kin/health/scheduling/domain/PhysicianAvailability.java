package com.kinplatform.kin.health.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Disponibilidad recurrente de un médico (horario semanal, ADR-034).
 *
 * <p>Define en qué día de la semana, entre qué horas y con qué duración de slot
 * el médico atiende. Un médico puede tener varios horarios (uno por día).</p>
 */
public record PhysicianAvailability(
        UUID id,
        UUID physicianId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean active,
        OffsetDateTime createdAt) {

    public PhysicianAvailability {
        if (id == null || physicianId == null || dayOfWeek == null) {
            throw new IllegalArgumentException("id/physicianId/dayOfWeek no pueden ser null");
        }
        startTime = startTime == null ? LocalTime.of(9, 0) : startTime;
        endTime = endTime == null ? LocalTime.of(17, 0) : endTime;
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime debe ser anterior a endTime");
        }
        slotDurationMinutes = slotDurationMinutes <= 0 ? 30 : slotDurationMinutes;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static PhysicianAvailability of(
            UUID id,
            UUID physicianId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            boolean active,
            OffsetDateTime createdAt) {
        return new PhysicianAvailability(
                id, physicianId, dayOfWeek, startTime, endTime, slotDurationMinutes, active, createdAt);
    }

    public boolean isActive() {
        return active;
    }
}
