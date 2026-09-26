package com.kinplatform.kin.health.hce.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterResponse {

    private UUID id;
    private UUID patientId;
    private UUID physicianId;
    private UUID organizationId;
    private UUID appointmentId;
    private String encounterType;
    private String status;
    private String chiefComplaint;
    private Instant startedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}