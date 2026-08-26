package com.kinplatform.kin.health.telemedicine.api;

import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cita de telemedicina en el endpoint REST (ADR-032).
 */
public record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID physicianId,
        OffsetDateTime scheduledAt,
        String reason,
        AppointmentStatus status,
        OffsetDateTime createdAt) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.id(),
                appointment.patientId(),
                appointment.physicianId(),
                appointment.scheduledAt(),
                appointment.reason(),
                appointment.status(),
                appointment.createdAt());
    }
}
