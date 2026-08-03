package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentArtifactMapperTest {

    private final DocumentArtifactMapper mapper = new DocumentArtifactMapper();

    @Test
    void roundTrip_conDocumentoCompleto_esExacto() {
        var original = EnterprisePersistenceTestFixtures.document(DocumentType.LEAN_CANVAS, 2);
        var project = new EnterpriseProjectEntity();
        project.setProjectId(UUID.randomUUID());
        project.setVersion(2);

        var entity = mapper.toEntity(original, project);
        var restored = mapper.toDomain(entity);

        assertEquals(original, restored);
        assertEquals(2, restored.version());
    }

    @Test
    void toEntity_mapeaProvenienciaYFormato() {
        var original = EnterprisePersistenceTestFixtures.document(DocumentType.KPI, 1);
        var project = new EnterpriseProjectEntity();
        project.setProjectId(UUID.randomUUID());
        project.setVersion(1);

        var entity = mapper.toEntity(original, project);

        assertEquals(DocumentType.KPI.name(), entity.getType());
        assertEquals("BusinessModelEngine", entity.getGeneratedBy());
        assertEquals("1.0.0", entity.getEngineVersion());
        assertEquals(RenderFormat.PDF.name(), entity.getRenderFormat());
        assertEquals("checksum-KPI", entity.getChecksum());
        assertTrue(entity.getMetadataJson().contains("origen"));
    }

    @Test
    void toDomain_laVersionProvieneDelProyecto() {
        var doc = EnterprisePersistenceTestFixtures.document(DocumentType.ROADMAP, 3);
        var project = new EnterpriseProjectEntity();
        project.setProjectId(UUID.randomUUID());
        project.setVersion(3);

        var restored = mapper.toDomain(mapper.toEntity(doc, project));

        assertEquals(3, restored.version());
    }

    @Test
    void toEntity_metadatosVacios_produceJsonNulo() {
        var doc = new DocumentArtifact(UUID.randomUUID(), DocumentType.DOFA, "contenido",
            java.time.OffsetDateTime.now(), "Motor", "1.0.0", "hash", 1,
            Map.of(), null, 0L, null, null);
        var project = new EnterpriseProjectEntity();

        var entity = mapper.toEntity(doc, project);

        assertNull(entity.getMetadataJson());
        var restored = mapper.toDomain(entity);
        assertTrue(restored.metadata().isEmpty());
        assertNull(restored.renderFormat());
        assertNull(restored.mimeType());
        assertNull(restored.checksum());
    }

    @Test
    void toEntity_conArtifactNulo_lanza() {
        var project = new EnterpriseProjectEntity();
        assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(null, project));
    }

    @Test
    void toDomain_conEntidadNula_lanza() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(null));
    }

    @Test
    void constructor_conObjectMapperNulo_lanza() {
        assertThrows(IllegalArgumentException.class,
            () -> new DocumentArtifactMapper(null));
    }

    @Test
    void toDomain_conMetadatosJsonInvalido_lanzaUncheckedIo() {
        var project = new EnterpriseProjectEntity();
        var entity = new DocumentArtifactEntity();
        entity.setId(UUID.randomUUID());
        entity.setProject(project);
        entity.setType("KPI");
        entity.setContent("contenido");
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setGeneratedBy("Motor");
        entity.setEngineVersion("1.0.0");
        entity.setInputHash("hash");
        entity.setSize(1L);
        entity.setMetadataJson("no-json");

        assertThrows(java.io.UncheckedIOException.class, () -> mapper.toDomain(entity));
    }

    @Test
    void toEntity_conErrorDeSerializacionDeMetadatos_lanzaUncheckedIo() {
        var failingMapper = new com.fasterxml.jackson.databind.ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonProcessingException("boom") {
                };
            }
        };
        var mapperConError = new DocumentArtifactMapper(failingMapper);
        var doc = EnterprisePersistenceTestFixtures.document(DocumentType.KPI, 1);

        assertThrows(java.io.UncheckedIOException.class,
            () -> mapperConError.toEntity(doc, new EnterpriseProjectEntity()));
    }
}
