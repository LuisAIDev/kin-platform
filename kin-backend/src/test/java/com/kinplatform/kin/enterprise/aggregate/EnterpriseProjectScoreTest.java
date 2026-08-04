package com.kinplatform.kin.enterprise.aggregate;

import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests del Enterprise Score en el aggregate (Fase 10, Milestone 3D): el score
 * se adjunta durante la generación ({@link EnterpriseProject#withScore}), se
 * conserva en las transiciones y se reconstruye con las fábricas sobrecargadas.
 */
class EnterpriseProjectScoreTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static EnterpriseScore score() {
        return EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);
    }

    @Test
    void request_nuncaPortaScore() {
        var project = EnterpriseProject.request(PROJECT_ID, 1);

        assertNull(project.score());
    }

    @Test
    void withScore_enRunning_adjuntaElScore() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();

        var scored = running.withScore(score());

        assertEquals(GenerationStatus.RUNNING, scored.status());
        assertEquals(score(), scored.score());
    }

    @Test
    void withScore_enRequested_lanza() {
        var requested = EnterpriseProject.request(PROJECT_ID, 1);

        assertThrows(EnterpriseProjectException.class, () -> requested.withScore(score()));
    }

    @Test
    void withScore_conScoreNulo_lanza() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();

        assertThrows(EnterpriseProjectException.class, () -> running.withScore(null));
    }

    @Test
    void completeGeneration_conservaElScore() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        var scored = running.withScore(score());

        var completed = scored.completeGeneration();

        assertEquals(GenerationStatus.COMPLETED, completed.status());
        assertEquals(score(), completed.score());
    }

    @Test
    void failGeneration_conservaElScore() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        var scored = running.withScore(score());

        var failed = scored.failGeneration("motivo");

        assertEquals(GenerationStatus.FAILED, failed.status());
        assertEquals(score(), failed.score());
    }

    @Test
    void attachDocument_conservaElScore() {
        var running = EnterpriseProject.request(PROJECT_ID, 1).startGeneration();
        var scored = running.withScore(score());

        var withDocument = scored.attachDocument(EnterpriseProjectTest.document(
            com.kinplatform.kin.enterprise.valueobjects.DocumentType.LEAN_CANVAS));

        assertEquals(score(), withDocument.score());
    }

    @Test
    void fabricaComplete_conScore_reconstruyeElAggregate() {
        var now = OffsetDateTime.now();

        var completed = EnterpriseProject.complete(PROJECT_ID, 1, now, now, now,
            List.of(), score());

        assertEquals(score(), completed.score());
        assertEquals(GenerationStatus.COMPLETED, completed.status());
    }

    @Test
    void fabricaStart_conScore_reconstruyeElAggregate() {
        var now = OffsetDateTime.now();

        var running = EnterpriseProject.start(PROJECT_ID, 1, now, now, List.of(), score());

        assertEquals(score(), running.score());
        assertEquals(GenerationStatus.RUNNING, running.status());
    }

    @Test
    void fabricasViejas_sinScore_conservanScoreNulo() {
        var now = OffsetDateTime.now();

        assertNull(EnterpriseProject.complete(PROJECT_ID, 1, now, now, now, List.of()).score());
        assertNull(EnterpriseProject.start(PROJECT_ID, 1, now, now, List.of()).score());
        assertNull(EnterpriseProject.fail(PROJECT_ID, 1, now, now, "motivo", List.of()).score());
    }
}

