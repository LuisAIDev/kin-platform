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
@Table(name = "mipres_supplies", indexes = {
    @Index(name = "idx_mipres_supplies_org", columnList = "organization_id"),
    @Index(name = "idx_mipres_supplies_prescription", columnList = "prescription_id"),
    @Index(name = "idx_mipres_supplies_supply_id", columnList = "supply_id", unique = true),
    @Index(name = "idx_mipres_supplies_date", columnList = "supply_date"),
    @Index(name = "idx_mipres_supplies_status", columnList = "status"),
    @Index(name = "idx_mipres_supplies_org_date", columnList = "organization_id, supply_date")
})
public class MipresSupply {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "supply_id", length = 50, unique = true)
    private String supplyId;

    @Column(name = "prescription_number", nullable = false, length = 20)
    private String prescriptionNumber;

    @Column(name = "supply_date", nullable = false)
    private LocalDate supplyDate;

    @Column(name = "cups_code", length = 20)
    private String cupsCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_value_cop", precision = 15, scale = 2)
    private BigDecimal unitValueCop;

    @Column(name = "total_value_cop", precision = 15, scale = 2)
    private BigDecimal totalValueCop;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplyStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_request", columnDefinition = "jsonb")
    private String rawRequest;

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

    public enum SupplyStatus {
        REPORTED, ANULLED, PENDING, REJECTED
    }

    public MipresSupply withStatus(SupplyStatus newStatus) {
        return this.toBuilder().status(newStatus).updatedAt(OffsetDateTime.now()).build();
    }
}