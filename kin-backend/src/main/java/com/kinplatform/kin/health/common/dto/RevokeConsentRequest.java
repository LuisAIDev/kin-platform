package com.kinplatform.kin.health.common.dto;

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
public class RevokeConsentRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Consent type is required")
    private String consentType;

    @NotNull(message = "Version is required")
    private String version;

    @Size(max = 1000, message = "Revocation reason cannot exceed 1000 characters")
    private String revocationReason;
}