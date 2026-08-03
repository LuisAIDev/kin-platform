package com.kinplatform.kin.enterprise.valueobjects;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Artefacto de documento del proyecto empresarial (value object).
 *
 * <p>Representa un documento generado dentro del aggregate
 * {@code EnterpriseProject}. Además del contenido neutral y el instante de
 * creación, porta la información mínima necesaria para la regeneración
 * inteligente y el renderizado: el motor que lo generó, la versión de dicho
 * motor, el hash de la entrada que lo produjo y la versión del proyecto
 * empresarial a la que pertenece, junto con metadatos opcionales, checksum,
 * tamaño en bytes, MIME type y formato de renderizado.</p>
 *
 * @param id            identificador único del artefacto
 * @param type          tipo de documento (catálogo {@link DocumentType})
 * @param content       contenido del documento (representación neutral)
 * @param createdAt     instante de generación del documento
 * @param generatedBy   motor de dominio que generó el documento
 * @param engineVersion versión del motor que generó el documento
 * @param inputHash     hash de la entrada que produjo el documento
 * @param version       versión del proyecto empresarial a la que pertenece
 * @param metadata      metadatos opcionales (inmutables)
 * @param checksum      checksum del contenido (o {@code null} si no se calculó)
 * @param size          tamaño del contenido en bytes (mayor o igual a 0)
 * @param mimeType      tipo MIME del contenido (o {@code null} si se desconoce)
 * @param renderFormat  formato de renderizado objetivo (o {@code null})
 */
public record DocumentArtifact(
    UUID id,
    DocumentType type,
    String content,
    OffsetDateTime createdAt,
    String generatedBy,
    String engineVersion,
    String inputHash,
    int version,
    Map<String, String> metadata,
    String checksum,
    long size,
    String mimeType,
    RenderFormat renderFormat
) {

    /**
     * Constructor canónico con validación completa de invariantes.
     */
    public DocumentArtifact {
        if (id == null) {
            throw new IllegalArgumentException("'id' no puede ser null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("'type' no puede ser null.");
        }
        ValueObjects.requireNotBlank(content, "content");
        if (createdAt == null) {
            throw new IllegalArgumentException("'createdAt' no puede ser null.");
        }
        ValueObjects.requireNotBlank(generatedBy, "generatedBy");
        ValueObjects.requireNotBlank(engineVersion, "engineVersion");
        ValueObjects.requireNotBlank(inputHash, "inputHash");
        ValueObjects.requireNonNegative(version, "version");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (checksum != null && checksum.isBlank()) {
            throw new IllegalArgumentException("'checksum' no puede ser vacío.");
        }
        ValueObjects.requireNonNegative(size, "size");
        if (mimeType != null && mimeType.isBlank()) {
            throw new IllegalArgumentException("'mimeType' no puede ser vacío.");
        }
    }

    /**
     * Constructor abreviado con los campos esenciales del Milestone 2B.
     *
     * <p>Mantiene la compatibilidad con el contrato original del aggregate
     * {@code EnterpriseProject}: inicializa metadatos vacíos, checksum y MIME
     * type a {@code null}, formato de renderizado a {@code null} y el tamaño
     * como la longitud en bytes UTF-8 del contenido.</p>
     */
    public DocumentArtifact(UUID id, DocumentType type, String content,
                            OffsetDateTime createdAt, String generatedBy,
                            String engineVersion, String inputHash, int version) {
        this(id, type, content, createdAt, generatedBy, engineVersion, inputHash, version,
            Map.of(), null, content == null ? 0L : content.getBytes(StandardCharsets.UTF_8).length,
            null, null);
    }

    /**
     * Crea un artefacto con el contenido y la trazabilidad mínima.
     */
    public static DocumentArtifact of(UUID id, DocumentType type, String content,
                                      OffsetDateTime createdAt, String generatedBy,
                                      String engineVersion, String inputHash, int version) {
        return new DocumentArtifact(id, type, content, createdAt, generatedBy,
            engineVersion, inputHash, version);
    }
}
