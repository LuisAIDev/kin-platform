package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.Referral.Priority;
import com.kinplatform.kin.health.hce.entity.Referral.ReferralType;
import com.kinplatform.kin.health.hce.entity.Referral.Status;
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
public class ReferralResponse {

    private UUID id;
    private UUID patientId;
    private UUID referringPhysicianId;
    private String referringService;
    private String referredToService;
    private String referredToInstitution;
    private UUID referredToPhysicianId;
    private ReferralType referralType;
    private Priority priority;
    private String reason;
    private String clinicalSummary;
    private Status status;
    private String counterreferralSummary;
    private String counterreferralRecommendations;
    private Instant counterreferralAt;
    private UUID counterreferralBy;
    private Instant scheduledAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}