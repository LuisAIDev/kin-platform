package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fixtures compartidos por los tests de los renderizadores (Fase 10,
 * Milestone 2H).
 */
final class RendererTestFixtures {

    private RendererTestFixtures() {
    }

    static DocumentArtifact document(DocumentType type, String content) {
        return new DocumentArtifact(UUID.randomUUID(), type, content, OffsetDateTime.now(),
            "BusinessModelEngine", "1.0.0", "hash-" + type, 1);
    }

    static DocumentArtifact document(DocumentType type) {
        return document(type, "contenido del documento " + type);
    }
}
