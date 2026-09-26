package com.kinplatform.kin.health.hce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateAnamnesisRequest {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    private Instant onsetDatetime;

    @Size(max = 5000, message = "Evolution description cannot exceed 5000 characters")
    private String evolutionDescription;

    @Size(max = 2000, message = "Aggravating factors cannot exceed 2000 characters")
    private String aggravatingFactors;

    @Size(max = 2000, message = "Alleviating factors cannot exceed 2000 characters")
    private String alleviatingFactors;

    @Size(max = 2000, message = "Associated symptoms cannot exceed 2000 characters")
    private String associatedSymptoms;

    @Min(value = 1, message = "Severity self-reported must be between 1 and 10")
    @Max(value = 10, message = "Severity self-reported must be between 1 and 10")
    private Integer severitySelfReported;

    private String systemsReview;

    @Min(value = 0, message = "Previous episodes cannot be negative")
    private Integer previousEpisodes;

    @Size(max = 2000, message = "Previous treatments cannot exceed 2000 characters")
    private String previousTreatments;

    @Size(max = 2000, message = "Functional impact cannot exceed 2000 characters")
    private String functionalImpact;
}