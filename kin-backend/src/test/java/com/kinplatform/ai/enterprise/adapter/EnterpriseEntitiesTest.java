package com.kinplatform.ai.enterprise.adapter;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobertura de los accesores y la igualdad generados de las entidades de
 * persistencia (Fase 10, Milestone 2G): los mapeadores construyen las entidades
 * con builders, por lo que estos tests ejercitan directamente los setters y la
 * identidad {@link EnterpriseProjectId}.
 */
class EnterpriseEntitiesTest {

    @Test
    void enterpriseProjectId_igualdadYHashCode() {
        var id = UUID.randomUUID();
        var a = new EnterpriseProjectId(id, 2);
        var b = new EnterpriseProjectId(id, 2);
        var c = new EnterpriseProjectId(id, 3);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(null, a);
        assertNotEquals("no-es-id", a);
        assertEquals(id, a.getProjectId());
        assertEquals(2, a.getVersion());
    }

    @Test
    void enterpriseProjectId_settersYNoArgs() {
        var id = new EnterpriseProjectId();
        id.setProjectId(UUID.randomUUID());
        id.setVersion(5);

        assertEquals(5, id.getVersion());
        assertTrue(id.toString().contains("projectId"));
    }

    @Test
    void enterpriseProjectId_igualdadConMismosValoresViaSetter() {
        var projectId = UUID.randomUUID();
        var a = new EnterpriseProjectId();
        a.setProjectId(projectId);
        a.setVersion(1);
        var b = new EnterpriseProjectId(projectId, 1);

        assertEquals(a, b);
    }

    @Test
    void enterpriseProjectId_igualdadReflexivaYConProjectIdDiferente() {
        var projectId = UUID.randomUUID();
        var a = new EnterpriseProjectId(projectId, 2);

        assertEquals(a, a);
        assertNotEquals(a, new EnterpriseProjectId(UUID.randomUUID(), 2));
    }

    @Test
    void enterpriseProjectId_conProjectIdNulo_enIgualdadYHashCode() {
        var a = new EnterpriseProjectId(null, 1);
        var b = new EnterpriseProjectId(null, 1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new EnterpriseProjectId(UUID.randomUUID(), 1));
        a.hashCode();
    }

    @Test
    void builders_generanToStringLegible() {
        String documento = DocumentArtifactEntity.builder()
            .id(UUID.randomUUID()).type("KPI").content("contenido").toString();
        String score = EnterpriseScoreEntity.builder().market(1.0).overall(50).toString();

        assertTrue(documento.contains("KPI"));
        assertTrue(score.contains("market"));
    }

    @Test
    void documentArtifactEntity_settersYGetter() {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var project = new EnterpriseProjectEntity();
        var entity = new DocumentArtifactEntity();
        entity.setId(id);
        entity.setProject(project);
        entity.setType("LEAN_CANVAS");
        entity.setContent("contenido");
        entity.setCreatedAt(now);
        entity.setGeneratedBy("Motor");
        entity.setEngineVersion("1.0.0");
        entity.setInputHash("hash");
        entity.setMetadataJson("{\"a\":\"b\"}");
        entity.setChecksum("checksum");
        entity.setSize(10L);
        entity.setMimeType("text/plain");
        entity.setRenderFormat("PDF");

        assertEquals(id, entity.getId());
        assertSame(project, entity.getProject());
        assertEquals("LEAN_CANVAS", entity.getType());
        assertEquals("contenido", entity.getContent());
        assertEquals(now, entity.getCreatedAt());
        assertEquals("Motor", entity.getGeneratedBy());
        assertEquals("1.0.0", entity.getEngineVersion());
        assertEquals("hash", entity.getInputHash());
        assertEquals("{\"a\":\"b\"}", entity.getMetadataJson());
        assertEquals("checksum", entity.getChecksum());
        assertEquals(10L, entity.getSize());
        assertEquals("text/plain", entity.getMimeType());
        assertEquals("PDF", entity.getRenderFormat());
    }

    @Test
    void enterpriseScoreEntity_settersYGetter() {
        var entity = new EnterpriseScoreEntity();
        entity.setMarket(10.0);
        entity.setInnovation(20.0);
        entity.setViability(30.0);
        entity.setFinancial(40.0);
        entity.setRisk(50.0);
        entity.setScalability(60.0);
        entity.setTeam(70.0);
        entity.setSustainability(80.0);
        entity.setOverall(45);
        entity.setConfidence(0.5);
        entity.setGrade("FAIR");

        assertEquals(10.0, entity.getMarket());
        assertEquals(20.0, entity.getInnovation());
        assertEquals(30.0, entity.getViability());
        assertEquals(40.0, entity.getFinancial());
        assertEquals(50.0, entity.getRisk());
        assertEquals(60.0, entity.getScalability());
        assertEquals(70.0, entity.getTeam());
        assertEquals(80.0, entity.getSustainability());
        assertEquals(45, entity.getOverall());
        assertEquals(0.5, entity.getConfidence());
        assertEquals("FAIR", entity.getGrade());
    }

    @Test
    void documentArtifactEntity_constructorCompleto() {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var project = new EnterpriseProjectEntity();

        var entity = new DocumentArtifactEntity(id, project, "KPI", "contenido", now,
            "Motor", "1.0.0", "hash", null, null, 9L, null, null);

        assertEquals(id, entity.getId());
        assertSame(project, entity.getProject());
        assertEquals("KPI", entity.getType());
        assertEquals("contenido", entity.getContent());
        assertEquals(9L, entity.getSize());
    }
}
