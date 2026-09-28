package com.kinplatform.kin.health.common.dto;

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
public class UserConsentResponse {

    private UUID id;
    private UUID userId;
    private String consentType;
    private String version;
    private Boolean accepted;
    private Instant acceptedAt;
    private Instant revokedAt;
    private String revocationReason;
    private String ipAddress;
    private String userAgent;
    private String documentHash;
    private Instant createdAt;
    private Instant updatedAt;
}