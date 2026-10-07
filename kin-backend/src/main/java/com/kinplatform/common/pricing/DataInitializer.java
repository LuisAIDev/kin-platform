package com.kinplatform.common.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed de planes comerciales de la vertical EMPRESAS (Fase 1):
 * FREE/STANDARD/PREMIUM.
 *
 * <p>Opera por {@code code} + {@code vertical = EMPRESAS} (no solo por
 * {@code code}): actualiza los planes canónicos si existen (preservando el id
 * y las FKs de suscripciones) y crea los que falten. Desde V37 el {@code code}
 * ya no es único globalmente (los planes de KIN Salud comparten códigos como
 * {@code FREE}), por lo que la búsqueda debe filtrar por vertical. Los
 * presupuestos de IA provienen de configuración (env {@code KIN_*_AI_BUDGET_USD})
 * como fuente inicial; el valor operativo vive en {@code PricingPlan.aiBudgetUsd}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final ProductVertical VERTICAL = ProductVertical.EMPRESAS;

    private final PricingPlanRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${kin.ai.free.budget-usd:0.50}")
    private BigDecimal freeAiBudget;

    @Value("${kin.ai.standard.budget-usd:6.25}")
    private BigDecimal standardAiBudget;

    @Value("${kin.ai.premium.budget-usd:8.75}")
    private BigDecimal premiumAiBudget;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        upsert(
                "FREE",
                "GRATIS",
                "Plan gratuito para empezar a evaluar tus ideas de negocio",
                BigDecimal.ZERO,
                List.of(
                        "3 proyectos completados por período",
                        "Asistente de IA incluido",
                        "Scoring de viabilidad",
                        "Exportación a PDF"),
                3,
                100,
                false,
                true,
                SupportLevel.BASIC,
                ViabilityScoringDetail.BASIC,
                freeAiBudget);

        upsert(
                "STANDARD",
                "STANDARD",
                "Plan ideal para emprendedores en crecimiento",
                new BigDecimal("25.00"),
                List.of(
                        "5 proyectos completados por período",
                        "IA avanzada",
                        "Scoring detallado",
                        "Exportación a PDF",
                        "Soporte prioritario"),
                5,
                500,
                true,
                true,
                SupportLevel.PREMIUM,
                ViabilityScoringDetail.DETAILED,
                standardAiBudget);

        upsert(
                "PREMIUM",
                "PREMIUM",
                "Plan completo: proyectos ilimitados y la mayor cuota de IA",
                new BigDecimal("35.00"),
                List.of(
                        "Proyectos ilimitados",
                        "IA avanzada",
                        "Scoring completo",
                        "Exportación a PDF",
                        "Soporte prioritario"),
                null,
                2000,
                true,
                true,
                SupportLevel.SUPPORT_24_7,
                ViabilityScoringDetail.DETAILED,
                premiumAiBudget);

        log.info("Planes comerciales sincronizados: FREE/STANDARD/PREMIUM");
    }

    private void upsert(
            String code,
            String name,
            String description,
            BigDecimal price,
            List<String> features,
            Integer maxProjects,
            Integer messagesPerMonth,
            boolean advancedAI,
            boolean pdfExport,
            SupportLevel supportLevel,
            ViabilityScoringDetail detail,
            BigDecimal aiBudget)
            throws Exception {
        Optional<PricingPlan> existing = repository.findByCodeAndVertical(code, VERTICAL);
        PricingPlan plan;
        if (existing.isPresent()) {
            plan = existing.get();
            plan.setName(name);
            plan.setDescription(description);
            plan.setPrice(price);
            plan.setFeatures(objectMapper.writeValueAsString(features));
            plan.setMaxProjects(maxProjects);
            plan.setMessagesPerMonth(messagesPerMonth);
            plan.setAdvancedAI(advancedAI);
            plan.setPdfExport(pdfExport);
            plan.setSupportLevel(supportLevel);
            plan.setViabilityScoringDetail(detail);
            plan.setIsActive(true);
            plan.setAiBudgetUsd(aiBudget);
            repository.save(plan);
            log.info("Plan {} actualizado (id={})", code, plan.getId());
        } else {
            plan = PricingPlan.builder()
                    .code(code)
                    .vertical(VERTICAL)
                    .name(name)
                    .description(description)
                    .price(price)
                    .features(objectMapper.writeValueAsString(features))
                    .maxProjects(maxProjects)
                    .messagesPerMonth(messagesPerMonth)
                    .advancedAI(advancedAI)
                    .pdfExport(pdfExport)
                    .supportLevel(supportLevel)
                    .viabilityScoringDetail(detail)
                    .isActive(true)
                    .aiBudgetUsd(aiBudget)
                    .build();
            repository.save(plan);
            log.info("Plan {} creado (id={})", code, plan.getId());
        }
    }
}

