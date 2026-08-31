package com.kinplatform.kin.health.documents.adapter;

import com.kinplatform.kin.health.documents.domain.DocumentStatus;
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

/**
 * Entidad JPA de documento clínico (tabla {@code clinical_documents}, ADR-036).
 */
@Entity
@Table(name = "clinical_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalDocumentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id")
    private UUID physicianId;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
