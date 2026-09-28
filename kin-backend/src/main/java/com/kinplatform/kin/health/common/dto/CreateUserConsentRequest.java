package com.kinplatform.kin.health.common.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateUserConsentRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Consent type is required")
    @Size(max = 50, message = "Consent type cannot exceed 50 characters")
    private String consentType;

    @NotBlank(message = "Version is required")
    @Size(max = 20, message = "Version cannot exceed 20 characters")
    private String version;

    @NotNull(message = "Accepted flag is required")
    private Boolean accepted;

    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    private String ipAddress;

    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    private String userAgent;

    @Size(max = 64, message = "Document hash cannot exceed 64 characters")
    private String documentHash;
}