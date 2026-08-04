package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseScoreSection;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests de la exposición del Enterprise Score en la capa web (Fase 10,
 * Milestone 3D): el dashboard devuelve la sección del score cuando la versión
 * lo porta y {@code null} en caso contrario.
 */
class EnterpriseWebMapperScoreTest {

    private final EnterpriseWebMapper mapper = new EnterpriseWebMapper();

    @Test
    void toDashboard_conScore_exponelaSeccionCompleta() {
        var projectId = UUID.randomUUID();
        var score = EnterpriseScore.calculate(70.0, 65.0, 80.0, 60.0, 50.0, 75.0, 68.0, 55.0, 0.82);
        var project = EnterpriseProject.complete(projectId, 1,
            OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now(),
            List.of(WebTestFixtures.document(
                com.kinplatform.kin.enterprise.valueobjects.DocumentType.KPI, 1)),
            score);

        var dashboard = mapper.toDashboard(project, List.of(project));

        assertNotNull(dashboard.score());
        assertEquals(65, dashboard.score().overall());
        assertEquals("FAIR", dashboard.score().grade());
        assertEquals(0.82, dashboard.score().confidence());
        assertEquals(70.0, dashboard.score().market());
        assertEquals(65.0, dashboard.score().innovation());
        assertEquals(80.0, dashboard.score().viability());
        assertEquals(60.0, dashboard.score().financial());
        assertEquals(50.0, dashboard.score().risk());
        assertEquals(75.0, dashboard.score().scalability());
        assertEquals(68.0, dashboard.score().team());
        assertEquals(55.0, dashboard.score().sustainability());
    }

    @Test
    void toDashboard_sinScore_devuelveScoreNulo() {
        var project = WebTestFixtures.completed(UUID.randomUUID(), 1,
            com.kinplatform.kin.enterprise.valueobjects.DocumentType.KPI);

        var dashboard = mapper.toDashboard(project, List.of(project));

        assertNull(dashboard.score());
    }

    @Test
    void toScoreSection_conScoreNulo_devuelveNull() {
        assertNull(mapper.toScoreSection(null));
    }

    @Test
    void toScoreSection_mapeaLasOchoDimensiones() {
        var score = EnterpriseScore.calculate(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 0.5);

        EnterpriseScoreSection section = mapper.toScoreSection(score);

        assertNotNull(section);
        assertEquals(45, section.overall());
        assertEquals(0.5, section.confidence());
        assertEquals(10.0, section.market());
        assertEquals(80.0, section.sustainability());
    }
}
