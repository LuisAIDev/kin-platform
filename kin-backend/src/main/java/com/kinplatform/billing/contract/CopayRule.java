package com.kinplatform.billing.contract;

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
@Table(name = "copay_rules", indexes = {
    @Index(name = "idx_copay_rules_contract", columnList = "contract_id"),
    @Index(name = "idx_copay_rules_priority", columnList = "contract_id, priority")
})
public class CopayRule {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "cups_code", length = 20)
    private String cupsCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cups_category", length = 50)
    private TariffCups.CupsCategory cupsCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_regimen", length = 20)
    private EpsContract.Regimen patientRegimen;

    @Column(name = "patient_age_min")
    private Integer patientAgeMin;

    @Column(name = "patient_age_max")
    private Integer patientAgeMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "copay_type", nullable = false, length = 20)
    private CopayType copayType;

    @Column(name = "copay_value_cop", precision = 14, scale = 2)
    private BigDecimal copayValueCop;

    @Column(name = "copay_cap_cop", precision = 14, scale = 2)
    private BigDecimal copayCapCop;

    @Column(name = "priority", nullable = false)
    private Integer priority;

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

    public enum CopayType { FIJO, PORCENTAJE, EXENTO, CUOTA_MODERADORA }
}