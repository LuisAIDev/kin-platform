package com.kinplatform.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_deletion_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataDeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_categories", columnDefinition = "JSONB")
    private JsonNode dataCategories;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "legal_hold")
    private Boolean legalHold = false;

    @Column(name = "legal_hold_reason", columnDefinition = "TEXT")
    private String legalHoldReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "execution_notes", columnDefinition = "TEXT")
    private String executionNotes;

    @Column(name = "anonymized_count")
    private Integer anonymizedCount = 0;

    @Column(name = "deleted_count")
    private Integer deletedCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}