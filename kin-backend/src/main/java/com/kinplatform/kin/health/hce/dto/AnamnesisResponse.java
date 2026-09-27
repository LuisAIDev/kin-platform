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
public class AnamnesisResponse {

    private UUID id;
    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private Instant onsetDatetime;
    private String evolutionDescription;
    private String aggravatingFactors;
    private String alleviatingFactors;
    private String associatedSymptoms;
    private Integer severitySelfReported;
    private String systemsReview;
    private Integer previousEpisodes;
    private String previousTreatments;
    private String functionalImpact;
    private Instant createdAt;
    private Instant updatedAt;
}
