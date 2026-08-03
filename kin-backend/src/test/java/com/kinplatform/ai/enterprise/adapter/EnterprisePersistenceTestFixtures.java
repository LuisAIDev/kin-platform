package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fixtures compartidos por los tests de persistencia del módulo Enterprise
 * (Fase 10, Milestone 2G).
 */
final class EnterprisePersistenceTestFixtures {

    private EnterprisePersistenceTestFixtures() {
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

    static List<DocumentArtifact> documents(int version) {
        return List.of(
            document(DocumentType.LEAN_CANVAS, version),
            document(DocumentType.MARKET_PLAN, version),
            document(DocumentType.FINANCIAL_PLAN, version));
    }

    static EnterpriseProject completed(UUID projectId, int version) {
        var now = OffsetDateTime.now();
        return EnterpriseProject.complete(projectId, version, now, now, now, documents(version));
    }

    static EnterpriseProject running(UUID projectId, int version) {
        var now = OffsetDateTime.now();
        return EnterpriseProject.start(projectId, version, now, now, documents(version));
    }

    static EnterpriseProject failed(UUID projectId, int version) {
        var now = OffsetDateTime.now();
        return EnterpriseProject.fail(projectId, version, now, now, "motivo de fallo", documents(version));
    }

    static EnterpriseScore score() {
        return EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);
    }
}
