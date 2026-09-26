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
public class CounterReferralRequest {

    @NotNull(message = "Counterreferral by physician ID is required")
    private UUID counterreferralBy;

    @Size(max = 5000, message = "Counterreferral summary cannot exceed 5000 characters")
    private String counterreferralSummary;

    @Size(max = 5000, message = "Counterreferral recommendations cannot exceed 5000 characters")
    private String counterreferralRecommendations;
}