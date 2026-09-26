package com.kinplatform.kin.health.hce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEncounterRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Encounter type is required")
    private String encounterType;

    @Size(max = 500, message = "Chief complaint cannot exceed 500 characters")
    private String chiefComplaint;

    private UUID appointmentId;
}