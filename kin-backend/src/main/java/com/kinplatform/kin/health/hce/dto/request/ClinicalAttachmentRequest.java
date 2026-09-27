package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
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
public class ClinicalAttachmentRequest {

    @NotNull
    @Pattern(regexp = "LAB_RESULT|IMAGING|PATHOLOGY|ENDOSCOPY|ELECTROCARDIOGRAM|OTHER")
    private String attachmentType;

    @NotNull
    @PastOrPresent
    private OffsetDateTime performedAt;

    @Size(max = 500)
    private String storageKey;

    @Size(max = 200)
    private String loincCode;

    @Size(max = 200)
    private String loincDisplay;

    @Size(max = 200)
    private String resultUnit;

    @Size(max = 200)
    private String referenceRangeText;

    private String dicomStudyUid;

    private String resultValue;

    @Size(max = 5000)
    private String resultText;
}