package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.DicomModality;
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
public class ClinicalAttachmentResponse {

    private UUID id;
    private UUID patientId;
    private UUID encounterId;
    private UUID evolutionId;
    private UUID orderId;
    private AttachmentType attachmentType;
    private String loincCode;
    private String loincDisplay;
    private String dicomStudyUid;
    private String dicomSeriesUid;
    private DicomModality dicomModality;
    private String pathologyCode;
    private BigDecimal resultValue;
    private String resultUnit;
    private String resultText;
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeText;
    private AbnormalFlag abnormalFlag;
    private String interpretation;
    private UUID documentId;
    private String storageKey;
    private String mimeType;
    private Instant performedAt;
    private Instant reportedAt;
    private UUID verifiedBy;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}