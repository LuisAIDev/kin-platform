package com.kinplatform.ai.enterprise.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Mapeador de documentos (Fase 10, Milestone 2G).
 *
 * <p>Convierte el value object de dominio {@link DocumentArtifact} en la
 * entidad {@link DocumentArtifactEntity} y viceversa. El tipo de documento y el
 * formato de renderizado se almacenan como strings agnósticos del dominio y los
 * metadatos como JSON (vía {@link ObjectMapper}), manteniendo la entidad sin
 * dependencias del dominio.</p>
 *
 * <p>La versión del documento no se mapea de forma independiente: la columna
 * {@code version} forma parte de la clave foránea de la asociación con el
 * proyecto, por lo que se obtiene de {@code project.getVersion()}.</p>
 */
public final class DocumentArtifactMapper {

    private final ObjectMapper objectMapper;

    public DocumentArtifactMapper() {
        this(new ObjectMapper());
    }

    /**
     * @param objectMapper mapeador JSON para los metadatos (obligatorio)
     */
    public DocumentArtifactMapper(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper no puede ser null");
        }
        this.objectMapper = objectMapper;
    }

    /**
     * Convierte un documento de dominio en la entidad, enlazada al proyecto.
     *
     * @param artifact documento de dominio (obligatorio)
     * @param project  entidad del proyecto al que pertenece (obligatorio)
     * @return la entidad del documento
     */
    public DocumentArtifactEntity toEntity(DocumentArtifact artifact, EnterpriseProjectEntity project) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact no puede ser null");
        }
        return DocumentArtifactEntity.builder()
            .id(artifact.id())
            .project(project)
            .type(artifact.type().name())
            .content(artifact.content())
            .createdAt(artifact.createdAt())
            .generatedBy(artifact.generatedBy())
            .engineVersion(artifact.engineVersion())
            .inputHash(artifact.inputHash())
            .metadataJson(toJson(artifact.metadata()))
            .checksum(artifact.checksum())
            .size(artifact.size())
            .mimeType(artifact.mimeType())
            .renderFormat(artifact.renderFormat() == null ? null : artifact.renderFormat().name())
            .build();
    }

    /**
     * Reconstruye un documento de dominio a partir de la entidad.
     *
     * @param entity entidad del documento (obligatoria)
     * @return el documento de dominio
     */
    public DocumentArtifact toDomain(DocumentArtifactEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity no puede ser null");
        }
        return new DocumentArtifact(
            entity.getId(),
            DocumentType.valueOf(entity.getType()),
            entity.getContent(),
            entity.getCreatedAt(),
            entity.getGeneratedBy(),
            entity.getEngineVersion(),
            entity.getInputHash(),
            entity.getProject().getVersion(),
            fromJson(entity.getMetadataJson()),
            entity.getChecksum(),
            entity.getSize(),
            entity.getMimeType(),
            entity.getRenderFormat() == null ? null : RenderFormat.valueOf(entity.getRenderFormat())
        );
    }

    private String toJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("No se pudo serializar los metadatos del documento", e);
        }
    }

    private Map<String, String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo deserializar los metadatos del documento", e);
        }
    }
}
