package com.kinplatform.billing.rips.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rips_generation_batches", indexes = {
    @Index(name = "idx_rips_batches_org_period", columnList = "organization_id, period_start, period_end"),
    @Index(name = "idx_rips_batches_contract_status", columnList = "contract_id, status")
})
public class RipsBatch {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "rips_type", nullable = false, length = 10)
    private RipsType ripsType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_hash_sha256", length = 64)
    private String fileHashSha256;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private String validationErrors;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "dian_response", columnDefinition = "jsonb")
    private String dianResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum RipsType { US, AF, AC, AP, AU, AT }
    public enum BatchStatus { GENERATING, VALIDATING, VALID, INVALID, SENT_TO_DIAN, ACCEPTED, REJECTED, CONTINGENCY }

    public void incrementRecordCount(int count) {
        this.recordCount = (this.recordCount == null ? 0 : this.recordCount) + count;
    }

    public void markInvalid(String errors) {
        this.status = BatchStatus.INVALID;
        this.validationErrors = errors;
        this.validatedAt = OffsetDateTime.now();
    }

    public void markValid() {
        this.status = BatchStatus.VALID;
        this.validatedAt = OffsetDateTime.now();
    }

    public void markError(String error) {
        this.status = BatchStatus.INVALID;
        this.validationErrors = error;
    }
}