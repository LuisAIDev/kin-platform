package com.kinplatform.kin.health.hce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokeConsentRequest {

    @NotBlank(message = "Revocation reason is required")
    private String reason;
}
