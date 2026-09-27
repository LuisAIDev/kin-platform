package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record AnamnesisRequest(
        @NotNull OffsetDateTime onsetDatetime,
        @Size(max = 2000) String evolutionDescription,
        @Min(1) @Max(10) Integer severitySelfReported
) {
}