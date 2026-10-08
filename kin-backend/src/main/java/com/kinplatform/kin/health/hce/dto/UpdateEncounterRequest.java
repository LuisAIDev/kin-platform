package com.kinplatform.kin.health.hce.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEncounterRequest {

    @Size(max = 500, message = "Chief complaint cannot exceed 500 characters")
    private String chiefComplaint;

    private String encounterType;
}
