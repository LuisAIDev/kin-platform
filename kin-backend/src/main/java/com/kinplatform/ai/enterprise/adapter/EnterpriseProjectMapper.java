package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.aggregate.GenerationStatus;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;

import java.util.List;

/**
 * Mapeador del proyecto empresarial (Fase 10, Milestone 2G).
 *
 * <p>Convierte el aggregate de dominio {@link EnterpriseProject} en la entidad
 * {@link EnterpriseProjectEntity} y viceversa, componiendo los mapeadores de
 * documentos ({@link DocumentArtifactMapper}) y de score
 * ({@link EnterpriseScoreMapper}). La entidad nunca conoce los tipos de dominio;
 * el estado se traduce a {@code String} y se reconstruye mediante las fábricas
 * del aggregate según el estado persistido.</p>
 *
 * <p>El score de dominio no forma parte del aggregate (contrato congelado), por
 * lo que la conversión a entidad lo deja en {@code null}: la capacidad de
 * persistencia del score queda soportada por la entidad y se ejercita a nivel
 * de repositorio. Los aggregates {@code REQUESTED} se reconstruyen con
 * {@link EnterpriseProject#request(java.util.UUID, int)} (timestamps del
 * instante de recarga, estado transitorio); {@code RUNNING}/{@code COMPLETED}/
 * {@code FAILED} conservan los timestamps persistidos mediante sus fábricas de
 * reconstrucción.</p>
 */
public final class EnterpriseProjectMapper {

    private final DocumentArtifactMapper documentMapper;
    private final EnterpriseScoreMapper scoreMapper;

    public EnterpriseProjectMapper() {
        this(new DocumentArtifactMapper(), new EnterpriseScoreMapper());
    }

    /**
     * @param documentMapper mapeador de documentos (obligatorio)
     * @param scoreMapper    mapeador de score (obligatorio)
     */
    public EnterpriseProjectMapper(DocumentArtifactMapper documentMapper, EnterpriseScoreMapper scoreMapper) {
        if (documentMapper == null) {
            throw new IllegalArgumentException("documentMapper no puede ser null");
        }
        if (scoreMapper == null) {
            throw new IllegalArgumentException("scoreMapper no puede ser null");
        }
        this.documentMapper = documentMapper;
        this.scoreMapper = scoreMapper;
    }

    /**
     * Convierte el aggregate de dominio en la entidad JPA.
     *
     * @param project proyecto empresarial de dominio (obligatorio)
     * @return la entidad JPA equivalente
     */
    public EnterpriseProjectEntity toEntity(EnterpriseProject project) {
        if (project == null) {
            throw new IllegalArgumentException("project no puede ser null");
        }
        EnterpriseProjectEntity entity = new EnterpriseProjectEntity();
        entity.setProjectId(project.projectId());
        entity.setVersion(project.version());
        entity.setStatus(project.status().name());
        entity.setCreatedAt(project.createdAt());
        entity.setUpdatedAt(project.updatedAt());
        entity.setCompletedAt(project.completedAt());
        entity.setFailedReason(project.failedReason());
        List<DocumentArtifactEntity> documents = project.documents().stream()
            .map(artifact -> documentMapper.toEntity(artifact, entity))
            .toList();
        entity.setDocuments(documents);
        return entity;
    }

    /**
     * Reconstruye el aggregate de dominio a partir de la entidad persistida.
     *
     * @param entity entidad JPA (obligatoria)
     * @return el aggregate de dominio equivalente
     */
    public EnterpriseProject toDomain(EnterpriseProjectEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity no puede ser null");
        }
        List<DocumentArtifact> documents = entity.getDocuments().stream()
            .map(documentMapper::toDomain)
            .toList();
        GenerationStatus status = GenerationStatus.valueOf(entity.getStatus());
        return switch (status) {
            case REQUESTED -> EnterpriseProject.request(entity.getProjectId(), entity.getVersion());
            case RUNNING -> EnterpriseProject.start(entity.getProjectId(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt(), documents);
            case COMPLETED -> EnterpriseProject.complete(entity.getProjectId(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCompletedAt(), documents);
            case FAILED -> EnterpriseProject.fail(entity.getProjectId(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getFailedReason(), documents);
        };
    }
}
