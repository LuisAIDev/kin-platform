package com.kinplatform.ai.enterprise.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA de un documento del proyecto empresarial (Fase 10, Milestone 2G).
 *
 * <p>Persiste un {@code DocumentArtifact} del aggregate
 * {@code EnterpriseProject}. Es el lado propietario de la relación
 * {@code @ManyToOne} con {@link EnterpriseProjectEntity}: la clave foránea
 * compuesta {@code (project_id, version)} referencia la versión del proyecto y
 * la unicidad de tipo se garantiza con la restricción
 * {@code UNIQUE (project_id, version, type)}.</p>
 *
 * <p>El tipo de documento, el formato de renderizado y los metadatos se
 * almacenan en forma agnóstica del dominio (strings y JSON); las conversiones
 * son responsabilidad del mapeador {@link DocumentArtifactMapper}. El campo
 * {@code version} no se modela como atributo propio: lo provee la asociación
 * con el proyecto (la misma columna forma parte de la clave foránea).</p>
 */
@Entity
@Table(name = "enterprise_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentArtifactEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "project_id", referencedColumnName = "project_id"),
        @JoinColumn(name = "version", referencedColumnName = "version")
    })
    private EnterpriseProjectEntity project;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "generated_by", nullable = false, length = 128)
    private String generatedBy;

    @Column(name = "engine_version", nullable = false, length = 32)
    private String engineVersion;

    @Column(name = "input_hash", nullable = false, length = 128)
    private String inputHash;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "render_format", length = 32)
    private String renderFormat;
}
