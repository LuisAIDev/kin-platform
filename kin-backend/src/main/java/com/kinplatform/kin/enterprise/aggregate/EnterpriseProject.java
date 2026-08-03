package com.kinplatform.kin.enterprise.aggregate;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root del proyecto empresarial de la Fase 10 (KIN Enterprise).
 *
 * <p>Agrega el conjunto de documentos generados para un proyecto de KIN a
 * partir de la conversación completada. Cada versión del proyecto empresarial
 * es una instancia distinta: {@code version} identifica la versión dentro del
 * mismo {@code projectId}.</p>
 *
 * <p>El Milestone 1 define únicamente la estructura (identidad, estado,
 * timestamps y colección de documentos) sin lógica de negocio: las
 * transiciones de estado ({@code REQUESTED → RUNNING → COMPLETED/FAILED}), el
 * versionado y las invariantes del aggregate se implementarán en los
 * milestones siguientes.</p>
 *
 * @param projectId  identificador del proyecto de KIN origen
 * @param version    versión del proyecto empresarial (1, 2, 3…)
 * @param status     estado actual de la generación
 * @param createdAt  instante de creación de la versión
 * @param updatedAt  instante de la última actualización de la versión
 * @param documents  documentos generados para esta versión
 */
public record EnterpriseProject(
    UUID projectId,
    int version,
    GenerationStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<DocumentArtifact> documents
) {

    public EnterpriseProject {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    /**
     * Colección inmutable de documentos de la versión.
     */
    public List<DocumentArtifact> documents() {
        return List.copyOf(documents);
    }
}
