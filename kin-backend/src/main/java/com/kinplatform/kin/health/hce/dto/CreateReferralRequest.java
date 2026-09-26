package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.Referral.Priority;
import com.kinplatform.kin.health.hce.entity.Referral.ReferralType;
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
public class CreateReferralRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Referring service is required")
    @Size(max = 100, message = "Referring service cannot exceed 100 characters")
    private String referringService;

    @NotNull(message = "Referred to service is required")
    @Size(max = 100, message = "Referred to service cannot exceed 100 characters")
    private String referredToService;

    @Size(max = 200, message = "Referred to institution cannot exceed 200 characters")
    private String referredToInstitution;

    private UUID referredToPhysicianId;

    @NotNull(message = "Referral type is required")
    private ReferralType referralType;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Reason is required")
    @Size(max = 2000, message = "Reason cannot exceed 2000 characters")
    private String reason;

    @Size(max = 5000, message = "Clinical summary cannot exceed 5000 characters")
    private String clinicalSummary;

    private java.time.Instant scheduledAt;
}