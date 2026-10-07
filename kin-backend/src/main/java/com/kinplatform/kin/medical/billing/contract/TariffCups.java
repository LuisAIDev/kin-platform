package com.kinplatform.kin.medical.billing.contract;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tariffs_cups", indexes = {
    @Index(name = "idx_tariffs_contract_cups", columnList = "contract_id, cups_code"),
    @Index(name = "idx_tariffs_contract_effective", columnList = "contract_id, effective_from, effective_to"),
    @Index(name = "idx_tariffs_category", columnList = "cups_category")
})
public class TariffCups {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "cups_code", nullable = false, length = 20)
    private String cupsCode;

    @Column(name = "cups_version", nullable = false, length = 10)
    private String cupsVersion;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "unit_price_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPriceCop;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "requires_auth", nullable = false)
    private Boolean requiresAuth;

    @Column(name = "auth_validity_days")
    private Integer authValidityDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "cups_category", length = 50)
    private CupsCategory cupsCategory;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

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

    public enum CupsCategory { CONSULTA, PROCEDIMIENTO, MEDICAMENTO, EXAMEN, INSUMO, DISPOSITIVO, OTRO }
}
