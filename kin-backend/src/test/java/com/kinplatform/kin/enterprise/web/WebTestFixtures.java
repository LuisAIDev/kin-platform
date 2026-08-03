package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fixtures compartidos por los tests de la API web Enterprise (Fase 10,
 * Milestone 2I).
 */
final class WebTestFixtures {

    private WebTestFixtures() {
    }

    static DocumentArtifact document(DocumentType type, int version) {
        String content = "contenido de " + type;
        return new DocumentArtifact(
            UUID.randomUUID(), type, content, OffsetDateTime.now(),
            "BusinessModelEngine", "1.0.0", "hash-" + type, version,
            Map.of("origen", "motor"), "checksum-" + type,
            content.getBytes(StandardCharsets.UTF_8).length,
            "text/plain", RenderFormat.PDF);
    }

    static EnterpriseProject completed(UUID projectId, int version, DocumentType... types) {
        List<DocumentArtifact> documents = new ArrayList<>();
        for (DocumentType type : types) {
            documents.add(document(type, version));
        }
        OffsetDateTime now = OffsetDateTime.now();
        return EnterpriseProject.complete(projectId, version, now, now, now, documents);
    }
}
