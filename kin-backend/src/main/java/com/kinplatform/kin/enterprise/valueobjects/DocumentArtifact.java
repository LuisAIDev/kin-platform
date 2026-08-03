package com.kinplatform.kin.enterprise.valueobjects;

import com.kinplatform.kin.enterprise.document.DocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Artefacto de documento del proyecto empresarial (value object).
 *
 * <p>Representa un documento generado dentro del aggregate
 * {@code EnterpriseProject}: identifica el tipo de documento y porta su
 * contenido (serializado de forma neutral) y su instante de creación. El
 * Milestone 1 define únicamente la estructura; la generación del contenido se
 * implementará en los milestones siguientes.</p>
 *
 * @param id        identificador único del artefacto
 * @param type      tipo de documento (catálogo {@link DocumentType})
 * @param content   contenido del documento (representación neutral)
 * @param createdAt instante de generación del documento
 */
public record DocumentArtifact(
    UUID id,
    DocumentType type,
    String content,
    OffsetDateTime createdAt
) {
}
