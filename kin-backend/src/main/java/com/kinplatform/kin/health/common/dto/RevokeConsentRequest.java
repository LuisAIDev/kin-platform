package com.kinplatform.kin.health.common.dto;

import jakarta.validation.constraints.NotNull;
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
}