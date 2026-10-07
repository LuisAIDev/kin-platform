package com.kinplatform.common.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.test.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test (C7): {@code PricingPlan.features} es una columna JSON
 * (jsonb en PostgreSQL, json en H2) mapeada con
 * {@code @JdbcTypeCode(SqlTypes.JSON)} sobre un String JSON. Verifica el
 * round-trip real del JSON contra PostgreSQL 18 (Testcontainers, con Flyway
 * V1..V11) y que DataInitializer siembra los planes sin el error "column
 * features is of type jsonb but expression is of type character varying".
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class PricingPlanFeaturesJpaTest extends PostgresTestSupport {

    @Autowired
    private PricingPlanRepository repository;

    @Autowired
    private EntityManager entityManager;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void dataInitializer_siembraPlanesConFeaturesComoJson() throws Exception {
        var initializer = new DataInitializer(repository, new ObjectMapper());
        initializer.run();

        assertEquals(3, repository.count());

        var free = repository.findByCode("FREE").orElseThrow();
        var freeFeatures = MAPPER.readValue(free.getFeatures(), new TypeReference<List<String>>() {});
        assertEquals(4, freeFeatures.size());
        assertTrue(freeFeatures.contains("Scoring de viabilidad"));

        var standard = repository.findByCode("STANDARD").orElseThrow();
        assertEquals(new BigDecimal("25.00"), standard.getPrice());
        assertEquals(5, standard.getMaxProjects());
        assertTrue(standard.getAdvancedAI());

        var premium = repository.findByCode("PREMIUM").orElseThrow();
        assertEquals(new BigDecimal("35.00"), premium.getPrice());
        assertEquals(null, premium.getMaxProjects());
        var premiumFeatures = MAPPER.readValue(premium.getFeatures(), new TypeReference<List<String>>() {});
        assertTrue(premiumFeatures.contains("Proyectos ilimitados"));
    }

    @Test
    void planesTienenBillingPeriodValido() throws Exception {
        var initializer = new DataInitializer(repository, new ObjectMapper());
        initializer.run();

        List<String> billingPeriods = entityManager
                .createNativeQuery(
                        "SELECT DISTINCT billing_period FROM pricing_plans WHERE code IN ('FREE','STANDARD','PREMIUM')")
                .getResultList();

        assertFalse(billingPeriods.isEmpty());
        for (Object bp : billingPeriods) {
            assertEquals("monthly", bp);
        }
    }

    @Test
    void features_json_roundTripComoStringJson() throws Exception {
        var features = "[\"Hasta 3 proyectos\",\"Exportación a PDF\"]";
        var plan = PricingPlan.builder()
                .name("Plan Prueba")
                .description("Descripción de prueba")
                .price(new BigDecimal("5.00"))
                .features(features)
                .maxProjects(3)
                .messagesPerMonth(10)
                .advancedAI(false)
                .pdfExport(false)
                .supportLevel(SupportLevel.BASIC)
                .viabilityScoringDetail(ViabilityScoringDetail.BASIC)
                .isActive(true)
                .build();

        repository.saveAndFlush(plan);
        entityManager.clear();

        var restored = repository.findByName("Plan Prueba").orElseThrow();
        var restoredFeatures = MAPPER.readValue(restored.getFeatures(), new TypeReference<List<String>>() {});
        assertEquals(List.of("Hasta 3 proyectos", "Exportación a PDF"), restoredFeatures);
    }
}

