package com.kinplatform.billing.authorization;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "authorizations", indexes = {
    @Index(name = "idx_authorizations_patient_status", columnList = "patient_id, status"),
    @Index(name = "idx_authorizations_contract_status", columnList = "contract_id, status"),
    @Index(name = "idx_authorizations_expiry", columnList = "expiry_date")
})
public class Authorization {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "authorization_number", nullable = false, length = 50)
    private String authorizationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_type", nullable = false, length = 30)
    private AuthorizationType authorizationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthorizationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cups_codes", nullable = false, columnDefinition = "jsonb")
    private String cupsCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_cie10", columnDefinition = "jsonb")
    private String diagnosisCie10;

    @Column(name = "requested_date", nullable = false)
    private OffsetDateTime requestedDate;

    @Column(name = "approved_date")
    private OffsetDateTime approvedDate;

    @Column(name = "expiry_date")
    private OffsetDateTime expiryDate;

    @Column(name = "approved_value_cop", precision = 14, scale = 2)
    private BigDecimal approvedValueCop;

    @Column(name = "used_value_cop", precision = 14, scale = 2)
    private BigDecimal usedValueCop;

    @Column(name = "notes", length = 2000)
    private String notes;

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

    public enum AuthorizationType { MIPRES, MANUAL, URGENCIA, PLAN_BENEFICIOS }
    public enum AuthorizationStatus { PENDING, APPROVED, PARTIAL, REJECTED, EXPIRED, USED }

    // Explicit builder for cross-compilation-unit visibility
    public static AuthorizationBuilder builder() {
        return new AuthorizationBuilder();
    }

    public static class AuthorizationBuilder {
        private UUID id;
        private UUID organizationId;
        private UUID contractId;
        private UUID patientId;
        private String authorizationNumber;
        private AuthorizationType authorizationType;
        private AuthorizationStatus status;
        private String cupsCodes;
        private String diagnosisCie10;
        private OffsetDateTime requestedDate;
        private OffsetDateTime approvedDate;
        private OffsetDateTime expiryDate;
        private BigDecimal approvedValueCop;
        private BigDecimal usedValueCop;
        private String notes;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public AuthorizationBuilder id(UUID id) { this.id = id; return this; }
        public AuthorizationBuilder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
        public AuthorizationBuilder contractId(UUID contractId) { this.contractId = contractId; return this; }
        public AuthorizationBuilder patientId(UUID patientId) { this.patientId = patientId; return this; }
        public AuthorizationBuilder authorizationNumber(String authorizationNumber) { this.authorizationNumber = authorizationNumber; return this; }
        public AuthorizationBuilder authorizationType(AuthorizationType authorizationType) { this.authorizationType = authorizationType; return this; }
        public AuthorizationBuilder status(AuthorizationStatus status) { this.status = status; return this; }
        public AuthorizationBuilder cupsCodes(String cupsCodes) { this.cupsCodes = cupsCodes; return this; }
        public AuthorizationBuilder diagnosisCie10(String diagnosisCie10) { this.diagnosisCie10 = diagnosisCie10; return this; }
        public AuthorizationBuilder requestedDate(OffsetDateTime requestedDate) { this.requestedDate = requestedDate; return this; }
        public AuthorizationBuilder approvedDate(OffsetDateTime approvedDate) { this.approvedDate = approvedDate; return this; }
        public AuthorizationBuilder expiryDate(OffsetDateTime expiryDate) { this.expiryDate = expiryDate; return this; }
        public AuthorizationBuilder approvedValueCop(BigDecimal approvedValueCop) { this.approvedValueCop = approvedValueCop; return this; }
        public AuthorizationBuilder usedValueCop(BigDecimal usedValueCop) { this.usedValueCop = usedValueCop; return this; }
        public AuthorizationBuilder notes(String notes) { this.notes = notes; return this; }
        public AuthorizationBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AuthorizationBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Authorization build() {
            Authorization auth = new Authorization();
            auth.id = this.id;
            auth.organizationId = this.organizationId;
            auth.contractId = this.contractId;
            auth.patientId = this.patientId;
            auth.authorizationNumber = this.authorizationNumber;
            auth.authorizationType = this.authorizationType;
            auth.status = this.status;
            auth.cupsCodes = this.cupsCodes;
            auth.diagnosisCie10 = this.diagnosisCie10;
            auth.requestedDate = this.requestedDate;
            auth.approvedDate = this.approvedDate;
            auth.expiryDate = this.expiryDate;
            auth.approvedValueCop = this.approvedValueCop;
            auth.usedValueCop = this.usedValueCop;
            auth.notes = this.notes;
            auth.createdAt = this.createdAt;
            auth.updatedAt = this.updatedAt;
            return auth;
        }
    }

    // Explicit getters/setters for cross-compilation-unit visibility (Lombok won't override)
    public AuthorizationStatus getStatus() { return status; }
    public void setStatus(AuthorizationStatus status) { this.status = status; }
    public void setApprovedDate(OffsetDateTime approvedDate) { this.approvedDate = approvedDate; }
    public void setExpiryDate(OffsetDateTime expiryDate) { this.expiryDate = expiryDate; }
    public void setApprovedValueCop(BigDecimal approvedValueCop) { this.approvedValueCop = approvedValueCop; }
    public OffsetDateTime getExpiryDate() { return expiryDate; }
    public AuthorizationType getAuthorizationType() { return authorizationType; }
}