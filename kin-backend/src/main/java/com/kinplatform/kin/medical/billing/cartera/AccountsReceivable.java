package com.kinplatform.kin.medical.billing.cartera;

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
@Table(name = "accounts_receivable", indexes = {
    @Index(name = "idx_ar_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_ar_org_bucket", columnList = "organization_id, aging_bucket"),
    @Index(name = "idx_ar_contract", columnList = "contract_id"),
    @Index(name = "idx_ar_due_date", columnList = "due_date")
})
public class AccountsReceivable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "fev_invoice_id")
    private UUID fevInvoiceId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_value_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalValueCop;

    @Column(name = "paid_value_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidValueCop;

    @Column(name = "pending_value_cop", insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal pendingValueCop;

    @Column(name = "days_overdue", nullable = false)
    private Integer daysOverdue;

    @Column(name = "aging_bucket", nullable = false, length = 20)
    private String agingBucket;

    @Column(name = "provision_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal provisionRate;

    @Column(name = "provision_value_cop", insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal provisionValueCop;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ArStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (paidValueCop == null) paidValueCop = BigDecimal.ZERO;
        if (totalValueCop == null) totalValueCop = BigDecimal.ZERO;
        if (daysOverdue == null) daysOverdue = 0;
        if (agingBucket == null) agingBucket = "CURRENT";
        if (provisionRate == null) provisionRate = BigDecimal.ZERO;
        if (status == null) status = ArStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum ArStatus {
        PENDING, PARTIAL, PAID, OVERDUE, WRITTEN_OFF
    }
}

