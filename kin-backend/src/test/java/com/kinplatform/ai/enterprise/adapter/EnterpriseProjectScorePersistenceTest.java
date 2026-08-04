package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.aggregate.GenerationStatus;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de la persistencia del Enterprise Score vía el adaptador JPA (Fase 10,
 * Milestone 3D): el mapper convierte el score del aggregate a la entidad y lo
 * reconstruye en el round-trip, sin perder las ocho dimensiones.
 */
class EnterpriseProjectScorePersistenceTest {

    private final EnterpriseProjectMapper mapper = new EnterpriseProjectMapper();

    @Test
    void toEntity_conScore_mapeaElScoreEmbedded() {
        var project = EnterpriseProject.complete(UUID.randomUUID(), 1,
            OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(),
            java.util.List.of(), EnterprisePersistenceTestFixtures.score());

        var entity = mapper.toEntity(project);

        assertNotNull(entity.getScore());
        assertEquals(70.0, entity.getScore().getMarket());
        assertEquals(65.0, entity.getScore().getInnovation());
        assertEquals(80.0, entity.getScore().getViability());
        assertEquals(60.0, entity.getScore().getFinancial());
        assertEquals(50.0, entity.getScore().getRisk());
        assertEquals(75.0, entity.getScore().getScalability());
        assertEquals(68.0, entity.getScore().getTeam());
        assertEquals(55.0, entity.getScore().getSustainability());
        assertEquals(65, entity.getScore().getOverall());
        assertEquals(0.82, entity.getScore().getConfidence());
        assertEquals("FAIR", entity.getScore().getGrade());
    }

    @Test
    void toEntity_sinScore_mapeaScoreNulo() {
        var project = EnterprisePersistenceTestFixtures.completed(UUID.randomUUID(), 1);

        var entity = mapper.toEntity(project);

        assertNull(entity.getScore());
    }

    @Test
    void roundTrip_conScore_esExacto() {
        var score = EnterprisePersistenceTestFixtures.score();
        var original = EnterpriseProject.complete(UUID.randomUUID(), 2,
            OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(),
            java.util.List.of(), score);

        var restored = mapper.toDomain(mapper.toEntity(original));

        assertEquals(GenerationStatus.COMPLETED, restored.status());
        assertTrue(restored.isCompleted());
        assertNotNull(restored.score());
        assertEquals(score, restored.score());
        assertEquals(score.overallScore(), restored.score().overallScore());
        assertEquals(score.grade(), restored.score().grade());
    }

    @Test
    void roundTrip_sinScore_conservaScoreNulo() {
        var original = EnterprisePersistenceTestFixtures.completed(UUID.randomUUID(), 1);

        var restored = mapper.toDomain(mapper.toEntity(original));

        assertNull(restored.score());
    }

    @Test
    void roundTrip_fallido_conScore_conservaElScore() {
        var score = EnterprisePersistenceTestFixtures.score();
        var now = OffsetDateTime.now();
        var original = EnterpriseProject.fail(UUID.randomUUID(), 1, now, now,
            "motivo de fallo", java.util.List.of(), score);

        var restored = mapper.toDomain(mapper.toEntity(original));

        assertEquals(GenerationStatus.FAILED, restored.status());
        assertEquals(score, restored.score());
    }
}
