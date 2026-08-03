package com.kinplatform.kin.enterprise.valueobjects;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Artefacto de documento del proyecto empresarial (value object).
 *
 * <p>Representa un documento generado dentro del aggregate
 * {@code EnterpriseProject}. Además del contenido neutral y el instante de
 * creación, porta la información mínima necesaria para la regeneración
 * inteligente: el motor que lo generó, la versión de dicho motor, el hash de
 * la entrada que lo produjo y la versión del proyecto empresarial a la que
 * pertenece. El Milestone 2A define únicamente el modelo; la generación del
 * contenido se implementará en los milestones siguientes.</p>
 *
 * @param id            identificador único del artefacto
 * @param type          tipo de documento (catálogo {@link DocumentType})
 * @param content       contenido del documento (representación neutral)
 * @param createdAt     instante de generación del documento
 * @param generatedBy   motor de dominio que generó el documento
 * @param engineVersion versión del motor que generó el documento
 * @param inputHash     hash de la entrada que produjo el documento
 * @param version       versión del proyecto empresarial a la que pertenece
 */
public record DocumentArtifact(
    UUID id,
    DocumentType type,
    String content,
    OffsetDateTime createdAt,
    String generatedBy,
    String engineVersion,
    String inputHash,
    int version
) {
}
