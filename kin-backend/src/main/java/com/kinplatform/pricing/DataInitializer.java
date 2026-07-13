package com.kinplatform.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PricingPlanRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (repository.count() > 0) {
            log.info("Pricing plans already seeded, skipping.");
            return;
        }

        var basicFeatures = List.of(
                "Hasta 3 proyectos",
                "Asistente de IA básico",
                "Scoring de viabilidad",
                "Exportación a PDF"
        );

        var premiumFeatures = List.of(
                "Proyectos ilimitados",
                "IA avanzada con contexto extendido",
                "Scoring detallado con métricas",
                "Exportación PDF premium",
                "Soporte prioritario 24/7"
        );

        var basic = PricingPlan.builder()
                .name("Básico Gratis")
                .price(BigDecimal.ZERO)
                .currency("USD")
                .billingPeriod("monthly")
                .features(objectMapper.writeValueAsString(basicFeatures))
                .isPopular(false)
                .displayOrder(1)
                .build();

        var premium = PricingPlan.builder()
                .name("Premium Pro")
                .price(new BigDecimal("19.00"))
                .currency("USD")
                .billingPeriod("monthly")
                .features(objectMapper.writeValueAsString(premiumFeatures))
                .isPopular(true)
                .displayOrder(2)
                .build();

        repository.saveAll(List.of(basic, premium));
        log.info("Seeded {} pricing plans", 2);
    }
}
