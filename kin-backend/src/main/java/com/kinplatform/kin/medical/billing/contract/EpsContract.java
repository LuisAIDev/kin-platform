package com.kinplatform.kin.medical.billing.contract;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
        name = "eps_contracts",
        indexes = {
            @Index(name = "idx_eps_contracts_org", columnList = "organization_id"),
            @Index(name = "idx_eps_contracts_status", columnList = "status")
        })
public class EpsContract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "eps_nit", nullable = false, length = 20)
    private String epsNit;

    @Column(name = "eps_name", nullable = false, length = 200)
    private String epsName;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen", nullable = false, length = 20)
    private Regimen regimen;

    @Column(name = "contract_number", length = 100)
    private String contractNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "payment_terms_days", nullable = false)
    private Integer paymentTermsDays;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "dian_resolution_number", length = 50)
    private String dianResolutionNumber;

    @Column(name = "dian_prefix", length = 10)
    private String dianPrefix;

    @Column(name = "dian_current_sequence", nullable = false)
    private Long dianCurrentSequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Regimen {
        CONTRIBUTIVO,
        SUBSIDIADO,
        ESPECIAL,
        EXCEPCION
    }

    public enum ContractStatus {
        ACTIVE,
        SUSPENDED,
        TERMINATED
    }

    public enum BillingCycle {
        WEEKLY,
        BIWEEKLY,
        MONTHLY
    }
}
