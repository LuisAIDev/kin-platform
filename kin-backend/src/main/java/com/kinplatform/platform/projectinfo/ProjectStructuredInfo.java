package com.kinplatform.platform.projectinfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dato estructurado del proyecto ({@code project_structured_info}): pares
 * {@code section/key/value} con origen explícito
 * ({@link StructuredInfoSourceType}). El valor ausente no se convierte en 0:
 * simplemente no existe una fila, o el dato se registra como "Por definir" /
 * "No disponible" cuando corresponda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "project_structured_info")
@IdClass(ProjectStructuredInfoId.class)
public class ProjectStructuredInfo {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(nullable = false, length = 64)
    private String section;

    @Id
    @Column(nullable = false, length = 64)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private StructuredInfoSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_source_type", length = 32)
    private StructuredInfoSourceType originalSourceType;

    @Column(name = "source_document", length = 255)
    private String sourceDocument;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = OffsetDateTime.now();
    }
}

