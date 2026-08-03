package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fixtures compartidos por los tests de exportación (Fase 10, Milestone 2H).
 */
final class ExportTestFixtures {

    private ExportTestFixtures() {
    }

    static DocumentArtifact document(DocumentType type, String content) {
        return new DocumentArtifact(UUID.randomUUID(), type, content, OffsetDateTime.now(),
            "BusinessModelEngine", "1.0.0", "hash-" + type, 1);
    }

    static EnterpriseProject project(UUID projectId, int version, DocumentType... types) {
        List<DocumentArtifact> documents = new ArrayList<>();
        for (DocumentType type : types) {
            documents.add(document(type, "contenido de " + type));
        }
        OffsetDateTime now = OffsetDateTime.now();
        return EnterpriseProject.complete(projectId, version, now, now, now, documents);
    }
}
