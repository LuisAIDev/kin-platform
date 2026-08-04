package com.kinplatform.pricing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test (C7): {@code PricingPlan.features} es una columna JSON
 * (jsonb en PostgreSQL, json en H2) mapeada con
 * {@code @JdbcTypeCode(SqlTypes.JSON)} sobre un String JSON. Verifica el
 * round-trip real del JSON contra la base y que DataInitializer siembra los
 * planes sin el error "column features is of type jsonb but expression is of
 * type character varying".
 */
@DataJpaTest
@ActiveProfiles("test")
class PricingPlanFeaturesJpaTest {

    @Autowired
    private PricingPlanRepository repository;

    @Autowired
    private EntityManager entityManager;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void dataInitializer_siembraPlanesConFeaturesComoJson() throws Exception {
        var initializer = new DataInitializer(repository, new ObjectMapper());
        initializer.run();

        assertEquals(2, repository.count());

        var basic = repository.findByName("Básico Gratis").orElseThrow();
        var basicFeatures = MAPPER.readValue(basic.getFeatures(), new TypeReference<List<String>>() {});
        assertEquals(5, basicFeatures.size());
        assertTrue(basicFeatures.contains("Scoring de viabilidad básico"));

        var premium = repository.findByName("Premium Pro").orElseThrow();
        var premiumFeatures = MAPPER.readValue(premium.getFeatures(), new TypeReference<List<String>>() {});
        assertEquals(6, premiumFeatures.size());
        assertTrue(premiumFeatures.contains("Soporte prioritario 24/7"));
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
