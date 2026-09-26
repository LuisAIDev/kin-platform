package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.DicomModality;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class CreateClinicalAttachmentRequest {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    private UUID evolutionId;

    private UUID orderId;

    @NotNull(message = "Attachment type is required")
    private AttachmentType attachmentType;

    @Size(max = 20, message = "LOINC code cannot exceed 20 characters")
    private String loincCode;

    @Size(max = 200, message = "LOINC display cannot exceed 200 characters")
    private String loincDisplay;

    @Size(max = 100, message = "DICOM study UID cannot exceed 100 characters")
    private String dicomStudyUid;

    @Size(max = 100, message = "DICOM series UID cannot exceed 100 characters")
    private String dicomSeriesUid;

    private DicomModality dicomModality;

    @Size(max = 50, message = "Pathology code cannot exceed 50 characters")
    private String pathologyCode;

    private BigDecimal resultValue;

    @Size(max = 50, message = "Result unit cannot exceed 50 characters")
    private String resultUnit;

    @Size(max = 5000, message = "Result text cannot exceed 5000 characters")
    private String resultText;

    private BigDecimal referenceRangeLow;

    private BigDecimal referenceRangeHigh;

    @Size(max = 200, message = "Reference range text cannot exceed 200 characters")
    private String referenceRangeText;

    private AbnormalFlag abnormalFlag;

    @Size(max = 2000, message = "Interpretation cannot exceed 2000 characters")
    private String interpretation;

    private UUID documentId;

    @Size(max = 500, message = "Storage key cannot exceed 500 characters")
    private String storageKey;

    @Size(max = 100, message = "MIME type cannot exceed 100 characters")
    private String mimeType;

    @NotNull(message = "Performed at is required")
    private Instant performedAt;

    private Instant reportedAt;

    private UUID verifiedBy;

    private Instant verifiedAt;
}