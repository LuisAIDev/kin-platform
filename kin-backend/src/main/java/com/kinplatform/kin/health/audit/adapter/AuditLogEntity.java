package com.kinplatform.kin.health.audit.adapter;

import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA de registro de auditoría (tabla {@code audit_logs}, ADR-035).
 * Los detalles se almacenan como JSONB (String JSON, patrón {@code patient_profiles}).
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private AuditResourceType resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
