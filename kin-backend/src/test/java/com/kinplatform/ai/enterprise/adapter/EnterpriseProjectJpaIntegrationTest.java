package com.kinplatform.ai.enterprise.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.aggregate.GenerationStatus;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.test.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de integración JPA de la persistencia del módulo Enterprise (Fase 10,
 * Milestone 2G) sobre PostgreSQL 18 real (Testcontainers, con Flyway V1..V11):
 * verifica el mapeo entidad ⇄ dominio, el versionado persistente, los
 * documentos en cascada y la persistencia del Enterprise Score contra una
 * base de datos real.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class EnterpriseProjectJpaIntegrationTest extends PostgresTestSupport {

    @Autowired
    private EnterpriseProjectJpaRepository jpaRepository;

    @Autowired
    private EntityManager entityManager;

    private EnterpriseProjectRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EnterpriseProjectRepositoryAdapter(jpaRepository);
    }

    /**
     * La migración V7 define {@code fk_enterprise_project_project → projects.id}
     * (H2 no la generaba en ddl-auto), por lo que cada test debe crear el
     * proyecto padre antes de persistir el enterprise_project. Se inserta por
     * SQL nativo para conservar exactamente el {@code id} asignado
     * ({@code @GeneratedValue(UUID)} regeneraría el id vía merge/persist).
     */
    private void seedParent(UUID projectId) {
        UUID userId = UUID.randomUUID();
        entityManager
                .createNativeQuery(
                        "INSERT INTO users (id, email, password_hash, full_name, role, credits, is_active, created_at, updated_at) "
                                + "VALUES (?, ?, 'seed', 'Proyecto semilla', 'FREE', 10, TRUE, now(), now())")
                .setParameter(1, userId)
                .setParameter(2, "seed-" + projectId + "@kin.test")
                .executeUpdate();
        entityManager
                .createNativeQuery("INSERT INTO projects (id, user_id, title, status, created_at, updated_at) "
                        + "VALUES (?, ?, 'Proyecto semilla', 'DRAFT', now(), now())")
                .setParameter(1, projectId)
                .setParameter(2, userId)
                .executeUpdate();
    }

    @Test
    void saveYfindLatestVersion_roundTripCompletoConDocumentos() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        var project = EnterprisePersistenceTestFixtures.completed(projectId, 1);

        adapter.save(project);
        entityManager.clear();

        var restored = adapter.findLatestVersion(projectId).orElseThrow();
        assertEquals(projectId, restored.projectId());
        assertEquals(1, restored.version());
        assertEquals(GenerationStatus.COMPLETED, restored.status());
        assertNotNull(restored.completedAt());
        assertSameInstant(project.createdAt(), restored.createdAt());
        assertSameInstant(project.updatedAt(), restored.updatedAt());
        assertSameInstant(project.completedAt(), restored.completedAt());
        assertEquals(3, restored.documentCount());
        assertTrue(restored.hasDocument(DocumentType.LEAN_CANVAS));
        assertTrue(restored.hasDocument(DocumentType.MARKET_PLAN));
        assertTrue(restored.hasDocument(DocumentType.FINANCIAL_PLAN));
        assertTrue(restored.findDocument(DocumentType.LEAN_CANVAS).isPresent());
        assertEquals(
                "checksum-LEAN_CANVAS",
                restored.findDocument(DocumentType.LEAN_CANVAS).get().checksum());
        assertEquals(
                "text/plain",
                restored.findDocument(DocumentType.LEAN_CANVAS).get().mimeType());
    }

    @Test
    void versionado_guardaVariasVersionesYRecuperaLaUltima() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 1));
        adapter.save(EnterprisePersistenceTestFixtures.failed(projectId, 2));

        var latest = adapter.findLatestVersion(projectId).orElseThrow();
        assertEquals(2, latest.version());
        assertEquals(GenerationStatus.FAILED, latest.status());

        var all = adapter.findAllVersions(projectId);
        assertEquals(2, all.size());
        assertEquals(1, all.get(0).version());
        assertEquals(2, all.get(1).version());
    }

    @Test
    void sobreescritura_deMismaVersion_actualizaLaFila() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 1));

        var now = OffsetDateTime.now();
        adapter.save(EnterpriseProject.fail(projectId, 1, now, now, "motivo nuevo", List.of()));

        var restored = adapter.findByVersion(projectId, 1).orElseThrow();
        assertEquals(GenerationStatus.FAILED, restored.status());
        assertEquals("motivo nuevo", restored.failedReason());
        assertEquals(1, adapter.findAllVersions(projectId).size());
        assertTrue(restored.documents().isEmpty());
    }

    @Test
    void findByVersion_devuelveLaVersionExacta() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 1));
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 2));

        var v1 = adapter.findByVersion(projectId, 1).orElseThrow();
        var v2 = adapter.findByVersion(projectId, 2).orElseThrow();

        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertTrue(adapter.findByVersion(projectId, 99).isEmpty());
    }

    @Test
    void persistenciaDeScore_guardaYRecuperaElScore() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        var now = OffsetDateTime.now();
        var entity = new EnterpriseProjectEntity();
        entity.setProjectId(projectId);
        entity.setVersion(1);
        entity.setStatus("COMPLETED");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCompletedAt(now);
        entity.setScore(new EnterpriseScoreMapper().toEmbedded(EnterprisePersistenceTestFixtures.score()));
        entity.setDocuments(new ArrayList<>());

        jpaRepository.saveAndFlush(entity);
        entityManager.clear();

        var loaded = jpaRepository.findByProjectIdAndVersion(projectId, 1).orElseThrow();
        var score = loaded.getScore();
        assertNotNull(score);
        assertEquals(70.0, score.getMarket());
        assertEquals(65.0, score.getInnovation());
        assertEquals(80.0, score.getViability());
        assertEquals(60.0, score.getFinancial());
        assertEquals(50.0, score.getRisk());
        assertEquals(75.0, score.getScalability());
        assertEquals(68.0, score.getTeam());
        assertEquals(55.0, score.getSustainability());
        assertEquals(65, score.getOverall());
        assertEquals("FAIR", score.getGrade());
        assertEquals(0.82, score.getConfidence());
        assertEquals(EnterprisePersistenceTestFixtures.score(), new EnterpriseScoreMapper().toDomain(score));
    }

    @Test
    void persistenciaDeScore_sinScore_recuperaNulo() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 1));

        var entity = jpaRepository.findByProjectIdAndVersion(projectId, 1).orElseThrow();
        assertNull(entity.getScore());
    }

    @Test
    void metadatosDeDocumentos_roundTrip() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 1));

        var restored = adapter.findLatestVersion(projectId).orElseThrow();
        var metadata =
                restored.findDocument(DocumentType.LEAN_CANVAS).orElseThrow().metadata();
        assertEquals("motor", metadata.get("origen"));
    }

    @Test
    void guardarProyectoRequested_sinDocumentos() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterpriseProject.request(projectId, 1));

        var restored = adapter.findLatestVersion(projectId).orElseThrow();
        assertEquals(GenerationStatus.REQUESTED, restored.status());
        assertTrue(restored.documents().isEmpty());
    }

    @Test
    void documentosConDistintosTipos_seConservanCompletos() {
        var projectId = UUID.randomUUID();
        seedParent(projectId);
        adapter.save(EnterprisePersistenceTestFixtures.completed(projectId, 1));

        var restored = adapter.findLatestVersion(projectId).orElseThrow();
        var types = restored.documents().stream().map(d -> d.type()).toList();
        assertTrue(types.containsAll(
                List.of(DocumentType.LEAN_CANVAS, DocumentType.MARKET_PLAN, DocumentType.FINANCIAL_PLAN)));
        assertEquals(3, types.size());
    }

    private static void assertSameInstant(OffsetDateTime expected, OffsetDateTime actual) {
        long nanos = Math.abs(java.time.Duration.between(expected, actual).toNanos());
        assertTrue(nanos < 2_000, "Los instantes difieren en " + nanos + " ns");
    }
}
