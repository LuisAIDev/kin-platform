package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentArtifactTest {

    private static final UUID ID = UUID.randomUUID();
    private static final DocumentType TYPE = DocumentType.LEAN_CANVAS;
    private static final String CONTENT = "contenido del documento";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now();
    private static final String GENERATED_BY = "LeanCanvasAssembler";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String INPUT_HASH = "abc123";
    private static final int VERSION = 3;

    @Test
    void of_deberiaCrearArtefactoConTrazabilidadMinima() {
        var artifact = DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION);

        assertEquals(ID, artifact.id());
        assertEquals(TYPE, artifact.type());
        assertEquals(CONTENT, artifact.content());
        assertEquals(CREATED_AT, artifact.createdAt());
        assertEquals(GENERATED_BY, artifact.generatedBy());
        assertEquals(ENGINE_VERSION, artifact.engineVersion());
        assertEquals(INPUT_HASH, artifact.inputHash());
        assertEquals(VERSION, artifact.version());
        assertEquals(Map.of(), artifact.metadata());
        assertNull(artifact.checksum());
        assertEquals(CONTENT.getBytes(StandardCharsets.UTF_8).length, artifact.size());
        assertNull(artifact.mimeType());
        assertNull(artifact.renderFormat());
    }

    @Test
    void constructorCompleto_deberiaGuardarTodosLosCampos() {
        var metadata = Map.of("clave", "valor");
        var artifact = new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
            metadata, "sha256:abcd", 42, "text/markdown", RenderFormat.PDF);

        assertEquals(metadata, artifact.metadata());
        assertEquals("sha256:abcd", artifact.checksum());
        assertEquals(42, artifact.size());
        assertEquals("text/markdown", artifact.mimeType());
        assertEquals(RenderFormat.PDF, artifact.renderFormat());
    }

    @Test
    void constructorConMetadataNull_deberiaUsarMapaVacio() {
        var artifact = new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
            null, null, 1, null, null);

        assertEquals(Map.of(), artifact.metadata());
    }

    @Test
    void metadata_deberiaSerInmutable() {
        var metadata = Map.of("clave", "valor");
        var artifact = new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
            metadata, null, 1, null, null);

        assertThrows(UnsupportedOperationException.class,
            () -> artifact.metadata().put("otra", "cosa"));
    }

    @Test
    void conIdNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(null, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION));
    }

    @Test
    void conTypeNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, null, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION));
    }

    @Test
    void conContentEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, "  ", CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION));
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, "", CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION));
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, null, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION));
    }

    @Test
    void conCreatedAtNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, CONTENT, null,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION));
    }

    @Test
    void conGeneratedByEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
                "", ENGINE_VERSION, INPUT_HASH, VERSION));
    }

    @Test
    void conEngineVersionEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, "", INPUT_HASH, VERSION));
    }

    @Test
    void conInputHashEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, "", VERSION));
    }

    @Test
    void conVersionNegativa_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, -1));
    }

    @Test
    void conChecksumVacio_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
                Map.of(), "", 1, null, null));
    }

    @Test
    void conChecksumNull_deberiaAceptarse() {
        var artifact = new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
            Map.of(), null, 1, null, null);
        assertNull(artifact.checksum());
    }

    @Test
    void conSizeNegativa_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
                Map.of(), null, -1, null, null));
    }

    @Test
    void conMimeTypeVacio_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
                GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
                Map.of(), null, 1, "", null));
    }

    @Test
    void conMimeTypeNull_deberiaAceptarse() {
        var artifact = new DocumentArtifact(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION,
            Map.of(), null, 1, null, null);
        assertNull(artifact.mimeType());
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var a = DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION);
        var b = DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION);
        var c = DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION + 1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "artifact");
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        assertNotNull(DocumentArtifact.of(ID, TYPE, CONTENT, CREATED_AT,
            GENERATED_BY, ENGINE_VERSION, INPUT_HASH, VERSION).toString());
    }
}
