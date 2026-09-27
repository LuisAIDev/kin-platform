package com.kinplatform.kin.health.hce.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EncounterResponse(
        UUID id,
        UUID patientId,
        UUID physicianId,
        UUID organizationId,
        UUID appointmentId,
        String encounterType,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime closedAt,
        String chiefComplaint,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}