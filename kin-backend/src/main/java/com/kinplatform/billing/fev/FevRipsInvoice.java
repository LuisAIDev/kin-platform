package com.kinplatform.billing.fev;

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
@Table(name = "fev_rips_invoices", indexes = {
    @Index(name = "idx_fev_rips_org_status", columnList = "organization_id, status"),
    @Index(name = "idx_fev_rips_batch", columnList = "batch_id"),
    @Index(name = "idx_fev_rips_contract", columnList = "contract_id")
})
public class FevRipsInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @Column(name = "invoice_prefix", nullable = false, length = 10)
    private String invoicePrefix;

    @Column(name = "invoice_sequence", nullable = false)
    private Long invoiceSequence;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "total_value_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalValueCop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "dian_cufe", length = 100)
    private String dianCufe;

    @Column(name = "dian_qr_code", columnDefinition = "text")
    private String dianQrCode;

    @Column(name = "signed_xml_path", length = 500)
    private String signedXmlPath;

    @Column(name = "signed_pdf_path", length = 500)
    private String signedPdfPath;

    @Column(name = "dian_response_xml", columnDefinition = "text")
    private String dianResponseXml;

    @Column(name = "contingency_reason", length = 500)
    private String contingencyReason;

    @Column(name = "contingency_deadline")
    private OffsetDateTime contingencyDeadline;

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

    public enum InvoiceStatus {
        DRAFT, SIGNED, SENT_TO_DIAN, ACCEPTED, REJECTED, CONTINGENCY, VOIDED
    }

    public void markSigned(String cufe, String qrCode) {
        this.status = InvoiceStatus.SIGNED;
        this.dianCufe = cufe;
        this.dianQrCode = qrCode;
    }

    public void markSent() {
        this.status = InvoiceStatus.SENT_TO_DIAN;
    }

    public void markAccepted(String responseXml) {
        this.status = InvoiceStatus.ACCEPTED;
        this.dianResponseXml = responseXml;
    }

    public void markRejected(String responseXml) {
        this.status = InvoiceStatus.REJECTED;
        this.dianResponseXml = responseXml;
    }

    public void markContingency(String reason, OffsetDateTime deadline) {
        this.status = InvoiceStatus.CONTINGENCY;
        this.contingencyReason = reason;
        this.contingencyDeadline = deadline;
    }
}
