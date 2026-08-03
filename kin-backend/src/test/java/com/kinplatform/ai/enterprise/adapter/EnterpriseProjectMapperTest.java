package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.aggregate.GenerationStatus;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseProjectMapperTest {

    private final EnterpriseProjectMapper mapper = new EnterpriseProjectMapper();

    @Test
    void toEntity_deProyectoCompletado_mapeaCabeceraYDocumentos() {
        var projectId = UUID.randomUUID();
        var project = EnterprisePersistenceTestFixtures.completed(projectId, 1);

        var entity = mapper.toEntity(project);

        assertEquals(projectId, entity.getProjectId());
        assertEquals(1, entity.getVersion());
        assertEquals("COMPLETED", entity.getStatus());
        assertEquals(project.createdAt(), entity.getCreatedAt());
        assertEquals(project.updatedAt(), entity.getUpdatedAt());
        assertEquals(project.completedAt(), entity.getCompletedAt());
        assertEquals(3, entity.getDocuments().size());
        assertNull(entity.getFailedReason());
        assertNull(entity.getScore());
    }

    @Test
    void toDomain_deEntidadCompletada_reconstruyeAggregate() {
        var projectId = UUID.randomUUID();
        var entity = mapper.toEntity(EnterprisePersistenceTestFixtures.completed(projectId, 2));

        var restored = mapper.toDomain(entity);

        assertEquals(projectId, restored.projectId());
        assertEquals(2, restored.version());
        assertEquals(GenerationStatus.COMPLETED, restored.status());
        assertTrue(restored.isCompleted());
        assertEquals(entity.getCompletedAt(), restored.completedAt());
        assertEquals(3, restored.documentCount());
        assertTrue(restored.hasDocument(DocumentType.LEAN_CANVAS));
        assertTrue(restored.hasDocument(DocumentType.FINANCIAL_PLAN));
    }

    @Test
    void roundTrip_deProyectoRunning_esExacto() {
        var projectId = UUID.randomUUID();
        var original = EnterprisePersistenceTestFixtures.running(projectId, 1);

        var restored = mapper.toDomain(mapper.toEntity(original));

        assertEquals(original.projectId(), restored.projectId());
        assertEquals(original.version(), restored.version());
        assertEquals(GenerationStatus.RUNNING, restored.status());
        assertEquals(original.createdAt(), restored.createdAt());
        assertEquals(original.updatedAt(), restored.updatedAt());
        assertEquals(original.documentCount(), restored.documentCount());
    }

    @Test
    void roundTrip_deProyectoFallido_esExacto() {
        var projectId = UUID.randomUUID();
        var original = EnterprisePersistenceTestFixtures.failed(projectId, 3);

        var restored = mapper.toDomain(mapper.toEntity(original));

        assertEquals(GenerationStatus.FAILED, restored.status());
        assertEquals("motivo de fallo", restored.failedReason());
        assertEquals(original.completedAt(), restored.completedAt());
        assertEquals(original.documentCount(), restored.documentCount());
    }

    @Test
    void toDomain_deProyectoRequested_conservaIdentidadYSinDocumentos() {
        var projectId = UUID.randomUUID();
        var entity = mapper.toEntity(EnterpriseProject.request(projectId, 1));

        var restored = mapper.toDomain(entity);

        assertEquals(projectId, restored.projectId());
        assertEquals(1, restored.version());
        assertEquals(GenerationStatus.REQUESTED, restored.status());
        assertTrue(restored.isRequested());
        assertTrue(restored.documents().isEmpty());
    }

    @Test
    void toDomain_deProyectoRequested_renuevaTimestamps() {
        var projectId = UUID.randomUUID();
        var original = EnterpriseProject.request(projectId, 1);
        var entity = mapper.toEntity(original);

        var restored = mapper.toDomain(entity);

        assertFalse(restored.createdAt().isBefore(original.createdAt()));
        assertTrue(restored.createdAt().isAfter(original.createdAt().minusSeconds(2)));
    }

    @Test
    void toEntity_conProyectoNulo_lanza() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(null));
    }

    @Test
    void toDomain_conEntidadNula_lanza() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(null));
    }

    @Test
    void constructor_conDependenciaNula_lanza() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectMapper(null, new EnterpriseScoreMapper()));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectMapper(new DocumentArtifactMapper(), null));
    }

    @Test
    void toEntity_estadoRequested_sinScoreYConTimestamps() {
        var projectId = UUID.randomUUID();
        var original = EnterpriseProject.request(projectId, 4);

        var entity = mapper.toEntity(original);

        assertEquals("REQUESTED", entity.getStatus());
        assertNull(entity.getCompletedAt());
        assertNull(entity.getFailedReason());
        assertEquals(original.createdAt(), entity.getCreatedAt());
        assertEquals(original.updatedAt(), entity.getUpdatedAt());
    }

    @Test
    void toDomain_conEstadoCompletado_conservaCompletedAtYCeroDocumentos() {
        var projectId = UUID.randomUUID();
        var completed = EnterpriseProject.complete(projectId, 5,
            OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusDays(1),
            OffsetDateTime.now(), java.util.List.of());

        var restored = mapper.toDomain(mapper.toEntity(completed));

        assertEquals(GenerationStatus.COMPLETED, restored.status());
        assertEquals(completed.completedAt(), restored.completedAt());
        assertTrue(restored.documents().isEmpty());
    }
}
