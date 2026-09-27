package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record ClinicalAttachmentRequest(
        @NotNull @Pattern(regexp = "LAB_RESULT|IMAGING|PATHOLOGY|ENDOSCOPY|ELECTROCARDIOGRAM|OTHER") String attachmentType,
        @NotNull @PastOrPresent OffsetDateTime performedAt,
        @Size(max = 500) String storageKey,
        @Size(max = 200) String loincCode,
        @Size(max = 200) String loincDisplay,
        @Size(max = 200) String resultUnit,
        @Size(max = 200) String referenceRangeText,
        String dicomStudyUid,
        String resultValue,
        @Size(max = 5000) String resultText
) {
}