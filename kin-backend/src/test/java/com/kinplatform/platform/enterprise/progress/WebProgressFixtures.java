package com.kinplatform.platform.enterprise.progress;

import com.kinplatform.platform.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.platform.enterprise.valueobjects.DocumentType;
import com.kinplatform.platform.enterprise.valueobjects.RenderFormat;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Fixtures compartidos por los tests de progreso (Fase 10, Milestone 2J).
 */
final class WebProgressFixtures {

    private WebProgressFixtures() {
    }

    static DocumentArtifact document(DocumentType type, int version) {
        String content = "contenido de " + type;
        return new DocumentArtifact(UUID.randomUUID(), type, content, OffsetDateTime.now(),
            "BusinessModelEngine", "1.0.0", "hash-" + type, version,
            Map.of(), null, content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
            "text/plain", RenderFormat.PDF);
    }
}


