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
                "Asistente de IA básico (DeepSeek V4 Flash)",
                "Scoring de viabilidad básico",
                "100 mensajes de IA por mes",
                "Exportación a PDF"
        );

        var premiumFeatures = List.of(
                "Proyectos ilimitados",
                "IA avanzada (DeepSeek V4 Pro)",
                "Scoring detallado con métricas avanzadas",
                "500 mensajes de IA por mes",
                "Exportación a PDF premium",
                "Soporte prioritario 24/7"
        );

        var basic = PricingPlan.builder()
                .name("Básico Gratis")
                .description("Plan gratuito para empezar a evaluar tus ideas de negocio")
                .price(BigDecimal.ZERO)
                .features(objectMapper.writeValueAsString(basicFeatures))
                .maxProjects(3)
                .messagesPerMonth(100)
                .advancedAI(false)
                .pdfExport(true)
                .supportLevel(SupportLevel.BASIC)
                .viabilityScoringDetail(ViabilityScoringDetail.BASIC)
                .isActive(true)
                .build();

        var premium = PricingPlan.builder()
                .name("Premium Pro")
                .description("Plan completo para emprendedores que buscan análisis profundo")
                .price(new BigDecimal("19.99"))
                .features(objectMapper.writeValueAsString(premiumFeatures))
                .maxProjects(null)
                .messagesPerMonth(500)
                .advancedAI(true)
                .pdfExport(true)
                .supportLevel(SupportLevel.SUPPORT_24_7)
                .viabilityScoringDetail(ViabilityScoringDetail.DETAILED)
                .isActive(true)
                .build();

        repository.saveAll(List.of(basic, premium));
        log.info("Seeded {} pricing plans", 2);
    }
}
