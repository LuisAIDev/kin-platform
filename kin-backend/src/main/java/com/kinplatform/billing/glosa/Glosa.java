package com.kinplatform.billing.glosa;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "glosas", indexes = {
    @Index(name = "idx_glosas_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_glosas_contract", columnList = "contract_id"),
    @Index(name = "idx_glosas_appeal_deadline", columnList = "appeal_deadline"),
    @Index(name = "idx_glosas_rips_record", columnList = "rips_record_id")
})
public class Glosa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "glosa_file_batch_id")
    private UUID glosaFileBatchId;

    @Column(name = "eps_glosa_number", length = 50)
    private String epsGlosaNumber;

    @Column(name = "rips_batch_id")
    private UUID ripsBatchId;

    @Column(name = "rips_record_id")
    private UUID ripsRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "glosa_type", nullable = false, length = 20)
    private GlosaType glosaType;

    @Column(name = "glosa_code", length = 20)
    private String glosaCode;

    @Column(name = "glosa_description", length = 500)
    private String glosaDescription;

    @Column(name = "original_value_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal originalValueCop;

    @Column(name = "glosa_value_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal glosaValueCop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GlosaStatus status;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "appeal_deadline")
    private OffsetDateTime appealDeadline;

    @Column(name = "appeal_submitted_at")
    private OffsetDateTime appealSubmittedAt;

    @Column(name = "appeal_arguments", columnDefinition = "text")
    private String appealArguments;

    @Column(name = "resolution_date")
    private OffsetDateTime resolutionDate;

    @Column(name = "resolved_value_cop", precision = 14, scale = 2)
    private BigDecimal resolvedValueCop;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = GlosaStatus.RECEIVED;
        if (originalValueCop == null) originalValueCop = BigDecimal.ZERO;
        if (glosaValueCop == null) glosaValueCop = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum GlosaType {
        VALOR, CODIGO, CANTIDAD, AUTORIZACION, VIGENCIA, DUPLICADO, OTRO
    }

    public enum GlosaStatus {
        RECEIVED, ANALYZING, APPEALING, APPEALED, ACCEPTED, REJECTED, CONCILIATED, WRITTEN_OFF
    }
}
