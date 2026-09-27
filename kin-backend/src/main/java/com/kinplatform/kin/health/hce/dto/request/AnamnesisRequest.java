package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnamnesisRequest {

    @NotNull
    private OffsetDateTime onsetDatetime;

    @Size(max = 2000)
    private String evolutionDescription;

    @Min(1)
    @Max(10)
    private Integer severitySelfReported;
}