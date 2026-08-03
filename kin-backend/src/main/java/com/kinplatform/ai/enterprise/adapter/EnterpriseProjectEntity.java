package com.kinplatform.ai.enterprise.adapter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA del proyecto empresarial (Fase 10, Milestone 2G).
 *
 * <p>Persiste el aggregate {@code EnterpriseProject} versionado por la clave
 * natural {@code (project_id, version)} ({@link EnterpriseProjectId}). Almacena
 * los datos de cabecera (estado, timestamps y motivo de fallo) en columnas
 * primitivas agnósticas del dominio, los documentos como {@code @OneToMany}
 * huérfanos con borrado en cascada y el {@code EnterpriseScore} como
 * {@code @Embedded} opcional en la misma fila.</p>
 *
 * <p>El estado se persiste como {@code String} (nunca se acopla al enum de
 * dominio); la conversión es responsabilidad del mapeador. Entidad de
 * infraestructura: no contiene lógica de negocio.</p>
 */
@Entity
@Table(name = "enterprise_project")
@IdClass(EnterpriseProjectId.class)
@Getter
@Setter
@NoArgsConstructor
public class EnterpriseProjectEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL,
        orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<DocumentArtifactEntity> documents = new ArrayList<>();

    @Embedded
    private EnterpriseScoreEntity score;
}
