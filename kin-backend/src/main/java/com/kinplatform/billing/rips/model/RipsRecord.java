package com.kinplatform.billing.rips.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rips_records", indexes = {
    @Index(name = "idx_rips_records_batch_seq", columnList = "batch_id, sequence_number"),
    @Index(name = "idx_rips_records_source", columnList = "source_entity_type, source_entity_id")
})
public class RipsRecord {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "source_entity_type", nullable = false, length = 30)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false)
    private UUID sourceEntityId;

    @Column(name = "rips_line_data", nullable = false, columnDefinition = "jsonb")
    private String ripsLineData;

    @Column(name = "validation_status", length = 20)
    private String validationStatus;

    @Column(name = "validation_error", length = 2000)
    private String validationError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}