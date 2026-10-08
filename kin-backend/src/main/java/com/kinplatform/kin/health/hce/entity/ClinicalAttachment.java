package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "clinical_attachments",
        indexes = {
            @Index(name = "idx_clinical_attachments_patient_id", columnList = "patient_id"),
            @Index(name = "idx_clinical_attachments_encounter_id", columnList = "encounter_id"),
            @Index(name = "idx_clinical_attachments_type", columnList = "attachment_type"),
            @Index(name = "idx_clinical_attachments_loinc", columnList = "loinc_code"),
            @Index(name = "idx_clinical_attachments_dicom", columnList = "dicom_study_uid"),
            @Index(name = "idx_clinical_attachments_performed_at", columnList = "performed_at")
        })
public class ClinicalAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    @Column(name = "evolution_id")
    private UUID evolutionId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 30)
    private AttachmentType attachmentType;

    @Column(name = "loinc_code", length = 20)
    private String loincCode;

    @Column(name = "loinc_display", length = 200)
    private String loincDisplay;

    @Column(name = "dicom_study_uid", length = 100)
    private String dicomStudyUid;

    @Column(name = "dicom_series_uid", length = 100)
    private String dicomSeriesUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "dicom_modality", length = 20)
    private DicomModality dicomModality;

    @Column(name = "pathology_code", length = 50)
    private String pathologyCode;

    @Column(name = "result_value", precision = 15, scale = 4)
    private BigDecimal resultValue;

    @Column(name = "result_unit", length = 50)
    private String resultUnit;

    @Column(name = "result_text", length = 5000)
    private String resultText;

    @Column(name = "reference_range_low", precision = 15, scale = 4)
    private BigDecimal referenceRangeLow;

    @Column(name = "reference_range_high", precision = 15, scale = 4)
    private BigDecimal referenceRangeHigh;

    @Column(name = "reference_range_text", length = 200)
    private String referenceRangeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "abnormal_flag", length = 10)
    private AbnormalFlag abnormalFlag;

    @Column(name = "interpretation", length = 2000)
    private String interpretation;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum AttachmentType {
        LAB_RESULT,
        IMAGING,
        PATHOLOGY,
        ENDOSCOPY,
        ELECTROCARDIOGRAM,
        OTHER
    }

    public enum DicomModality {
        CT,
        MR,
        XR,
        US,
        NM,
        PT,
        OT,
        CR,
        DX,
        MG,
        IO,
        PX,
        RF,
        HC,
        ES,
        LS,
        ST,
        GM,
        BD,
        BI,
        CD,
        CF,
        CP,
        CS,
        DD,
        DG,
        DM,
        EC,
        EPS,
        FA,
        FS,
        GS,
        HD,
        IVUS,
        IVOCT,
        IVUSOCT,
        KER,
        KO,
        LEN,
        LN,
        MB,
        MRA,
        MRV,
        MS,
        OP,
        OPM,
        OPT,
        OPV,
        PAT,
        PTX,
        REG,
        RES,
        RFA,
        RTDOSE,
        RTIMAGE,
        RTPLAN,
        RTRECORD,
        RTSTRUCT,
        RWV,
        SD,
        SMR,
        SPC,
        SR,
        SRF,
        TD,
        TG,
        VA,
        XA,
        XC
    }

    public enum AbnormalFlag {
        NORMAL,
        HIGH,
        LOW,
        CRITICAL,
        ABNORMAL
    }
}
