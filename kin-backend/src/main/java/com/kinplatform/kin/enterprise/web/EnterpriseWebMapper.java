package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.application.EnterpriseDocumentBundle;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationRequest;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseDocumentResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseExportResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseGenerateRequest;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseProjectResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseProjectSummaryResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseStatusResponse;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseVersionResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapeador REST del módulo Enterprise (Fase 10, Milestone 2I).
 *
 * <p>Convierte los objetos de dominio del proyecto empresarial
 * ({@link EnterpriseProject}, {@link DocumentArtifact},
 * {@link EnterpriseDocumentBundle}) en sus representaciones DTO de la API y
 * viceversa (solicitud de generación). Clase stateless y thread-safe: no
 * contiene lógica de negocio, solo transformación de representación.</p>
 */
public final class EnterpriseWebMapper {

    /**
     * Convierte una versión del proyecto en su representación completa.
     *
     * @param project proyecto empresarial (obligatorio)
     * @return DTO de proyecto completo
     */
    public EnterpriseProjectResponse toResponse(EnterpriseProject project) {
        requireProject(project);
        return new EnterpriseProjectResponse(
            project.projectId(),
            project.version(),
            project.status().name(),
            project.createdAt(),
            project.updatedAt(),
            project.completedAt(),
            project.failedReason(),
            project.documentCount(),
            toDocuments(project.documents()));
    }

    /**
     * Convierte una versión del proyecto en su resumen ligero.
     *
     * @param project proyecto empresarial (obligatorio)
     * @return DTO de resumen
     */
    public EnterpriseProjectSummaryResponse toSummary(EnterpriseProject project) {
        requireProject(project);
        return new EnterpriseProjectSummaryResponse(
            project.projectId(),
            project.version(),
            project.status().name(),
            project.documentCount(),
            project.updatedAt());
    }

    /**
     * Convierte una versión del proyecto en una entrada de listado de versiones.
     *
     * @param project proyecto empresarial (obligatorio)
     * @return DTO de versión
     */
    public EnterpriseVersionResponse toVersion(EnterpriseProject project) {
        requireProject(project);
        return new EnterpriseVersionResponse(
            project.version(),
            project.status().name(),
            project.createdAt(),
            project.updatedAt(),
            project.completedAt(),
            project.failedReason(),
            project.documentCount());
    }

    /**
     * Convierte una versión del proyecto en su estado.
     *
     * @param project proyecto empresarial (obligatorio)
     * @return DTO de estado
     */
    public EnterpriseStatusResponse toStatus(EnterpriseProject project) {
        requireProject(project);
        return new EnterpriseStatusResponse(
            project.projectId(),
            project.version(),
            project.status().name(),
            project.updatedAt(),
            project.completedAt(),
            project.failedReason());
    }

    /**
     * Convierte un documento en sus metadatos REST.
     *
     * @param artifact documento de dominio (obligatorio)
     * @return DTO de documento
     */
    public EnterpriseDocumentResponse toDocument(DocumentArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact no puede ser null");
        }
        return new EnterpriseDocumentResponse(
            artifact.id(),
            artifact.type().name(),
            artifact.size(),
            artifact.createdAt(),
            artifact.generatedBy(),
            artifact.engineVersion(),
            artifact.version(),
            artifact.inputHash(),
            artifact.renderFormat() == null ? null : artifact.renderFormat().name(),
            artifact.mimeType(),
            artifact.checksum());
    }

    /**
     * Convierte la colección de documentos de una versión.
     *
     * @param artifacts documentos de la versión (obligatorio)
     * @return lista de DTOs de documento
     */
    public List<EnterpriseDocumentResponse> toDocuments(List<DocumentArtifact> artifacts) {
        if (artifacts == null) {
            throw new IllegalArgumentException("artifacts no puede ser null");
        }
        return artifacts.stream().map(this::toDocument).toList();
    }

    /**
     * Convierte el bundle de exportación en su resumen (tamaños por formato).
     *
     * @param bundle bundle inmutable de exportación (obligatorio)
     * @return DTO de resumen de exportación
     */
    public EnterpriseExportResponse toExport(EnterpriseDocumentBundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle no puede ser null");
        }
        Map<String, Map<String, Long>> documents = new LinkedHashMap<>();
        for (Map.Entry<DocumentType, Map<RenderFormat, byte[]>> entry : bundle.documents().entrySet()) {
            Map<String, Long> formats = new LinkedHashMap<>();
            for (Map.Entry<RenderFormat, byte[]> rendering : entry.getValue().entrySet()) {
                formats.put(rendering.getKey().name(), (long) rendering.getValue().length);
            }
            documents.put(entry.getKey().name(), Map.copyOf(formats));
        }
        return new EnterpriseExportResponse(
            bundle.projectId(), bundle.version(), Map.copyOf(documents));
    }

    /**
     * Convierte la solicitud REST de generación en la solicitud de dominio,
     * incorporando el contexto durable del proyecto.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param dto       solicitud REST (obligatoria)
     * @param context   contexto durable del proyecto (obligatorio)
     * @return solicitud de dominio para el orquestador de generación
     */
    public EnterpriseGenerationRequest toDomain(UUID projectId, EnterpriseGenerateRequest dto,
                                                ProjectContext context) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("dto no puede ser null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context no puede ser null");
        }
        return new EnterpriseGenerationRequest(projectId, context, null, null, null, null);
    }

    private void requireProject(EnterpriseProject project) {
        if (project == null) {
            throw new IllegalArgumentException("project no puede ser null");
        }
    }
}
