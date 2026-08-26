package com.kinplatform.kin.health.differential.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.test.PostgresTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del adaptador JPA del diagnóstico diferencial con PostgreSQL real
 * (Testcontainers). Verifica que Flyway V23 crea las tablas y que el catálogo
 * se carga con los datos sembrados.
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaDifferentialRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    private JpaDifferentialKnowledgeRepository knowledgeRepository;

    @Test
    @Transactional
    void catalogo_deberiaCargarFactoresDeRiesgoYPruebas() {
        DifferentialCatalog catalog = knowledgeRepository.loadCatalog();

        assertFalse(catalog.isEmpty());
        assertTrue(catalog.riskFactors().size() >= 20, "debe haber al menos 20 factores de riesgo");
        assertTrue(catalog.recommendedTests().size() >= 20, "debe haber al menos 20 pruebas recomendadas");

        // Gripe debe tener factores de riesgo y pruebas
        UUID gripeId = UUID.fromString("22220000-0000-0000-0000-000000010002");
        assertFalse(catalog.riskFactorsFor(gripeId).isEmpty(), "Gripe debe tener factores de riesgo");
        assertFalse(catalog.recommendedTestsFor(gripeId).isEmpty(), "Gripe debe tener pruebas recomendadas");
    }

    @Test
    @Transactional
    void applyUpdate_deberiaAgregarElementosNuevosYSerIdempotente() {
        var conditionId = UUID.fromString("22220000-0000-0000-0000-000000010002");
        var riskId =
                com.kinplatform.kin.engine.DeterministicId.from("risk-factor", conditionId.toString(), "test-factor");
        var testId = com.kinplatform.kin.engine.DeterministicId.from(
                "recommended-test", conditionId.toString(), "test-prueba");
        var update = com.kinplatform.kin.health.differential.domain.CatalogUpdate.of(
                "test-source",
                List.of(com.kinplatform.kin.health.differential.domain.RiskFactor.of(
                        riskId, conditionId, "test-factor", 0.2, "desc")),
                List.of(com.kinplatform.kin.health.differential.domain.RecommendedTest.of(
                        testId, conditionId, "test-prueba", "desc")));

        var first = knowledgeRepository.applyUpdate(update);
        var second = knowledgeRepository.applyUpdate(update);

        assertEquals(1, first.riskFactorsAdded());
        assertEquals(1, first.testsAdded());
        assertEquals(0, second.riskFactorsAdded());
        assertEquals(0, second.testsAdded());
    }
}
