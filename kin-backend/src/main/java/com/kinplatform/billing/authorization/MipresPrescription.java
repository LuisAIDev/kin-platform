package com.kinplatform.billing.authorization;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mipres_prescriptions", indexes = {
    @Index(name = "idx_mipres_prescriptions_org", columnList = "organization_id"),
    @Index(name = "idx_mipres_prescriptions_number", columnList = "prescription_number", unique = true),
    @Index(name = "idx_mipres_prescriptions_patient", columnList = "patient_id"),
    @Index(name = "idx_mipres_prescriptions_created", columnList = "created_at"),
    @Index(name = "idx_mipres_prescriptions_status", columnList = "status"),
    @Index(name = "idx_mipres_prescriptions_org_status", columnList = "organization_id, status")
})
public class MipresPrescription {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "prescription_number", nullable = false, length = 20, unique = true)
    private String prescriptionNumber;

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PrescriptionStatus status;

    @Column(name = "cups_code", length = 20)
    private String cupsCode;

    @Column(name = "diagnosis_cie10", length = 10)
    private String diagnosisCie10;

    @Column(name = "qty_approved")
    private Integer qtyApproved;

    @Column(name = "unit_price_cop", precision = 15, scale = 2)
    private BigDecimal unitPriceCop;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

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

    public enum PrescriptionStatus {
        PENDING, AUTHORIZED, REJECTED, EXPIRED, CONSUMED
    }

    public MipresPrescription withStatus(PrescriptionStatus newStatus) {
        return this.toBuilder().status(newStatus).updatedAt(OffsetDateTime.now()).build();
    }
}