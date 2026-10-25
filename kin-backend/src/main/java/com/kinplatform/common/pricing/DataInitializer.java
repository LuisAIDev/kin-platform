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
 * {@code code}): actualiza los metadatos de los planes canónicos si existen
 * (preservando el id, las FKs de suscripciones y los precios gestionados por
 * Flyway) y crea los que falten. Desde V37 el {@code code}
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

    /**
     * Precios aprobados para EMPRESAS. Solo se usan al CREAR un plan faltante
     * (instalación nueva). Si el plan ya existe, sus precios son propiedad de
     * Flyway (V85) y este inicializador no los toca.
     */
    private static final BigDecimal FREE_PRICE = new BigDecimal("0.00");

    private static final BigDecimal FREE_PRICE_COP = new BigDecimal("0.00");
    private static final BigDecimal STANDARD_PRICE = new BigDecimal("15.00");
    private static final BigDecimal STANDARD_PRICE_COP = new BigDecimal("60000.00");
    private static final BigDecimal PREMIUM_PRICE = new BigDecimal("22.00");
    private static final BigDecimal PREMIUM_PRICE_COP = new BigDecimal("88000.00");

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
                FREE_PRICE,
                FREE_PRICE_COP,
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
                STANDARD_PRICE,
                STANDARD_PRICE_COP,
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
                PREMIUM_PRICE,
                PREMIUM_PRICE_COP,
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
            BigDecimal priceCop,
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
        boolean adoptLegacy = false;
        if (existing.isEmpty()) {
            // Bases antiguas pueden tener planes EMPRESAS sin `code` (p. ej.
            // "GRATIS"/"STANDARD"). Se adoptan por nombre en lugar de crear un
            // duplicado que dejaría tarjetas repetidas y sin price_cop.
            existing = repository.findFirstByCodeIsNullAndNameAndVertical(name, VERTICAL);
            if (existing.isEmpty() && !name.equals(code)) {
                // Algunas bases usan el propio code como nombre del plan legacy.
                existing = repository.findFirstByCodeIsNullAndNameAndVertical(code, VERTICAL);
            }
            adoptLegacy = existing.isPresent();
        }

        PricingPlan plan;
        if (existing.isPresent()) {
            plan = existing.get();
            if (adoptLegacy) {
                // El plan legacy no pasó por V85 (que filtra por code): al
                // adoptarlo le aplicamos el code y los precios aprobados.
                plan.setCode(code);
                plan.setPrice(price);
                plan.setPriceCop(priceCop);
                log.info("Plan legacy {} adoptado como {} (id={})", name, code, plan.getId());
            }
            plan.setName(name);
            plan.setDescription(description);
            // price y price_cop son propiedad de Flyway (V85): no se sobrescriben
            // salvo al adoptar un plan legacy, que V85 no pudo actualizar.
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
            log.info("Plan {} actualizado sin tocar precios (id={})", code, plan.getId());
        } else {
            plan = PricingPlan.builder()
                    .code(code)
                    .vertical(VERTICAL)
                    .name(name)
                    .description(description)
                    .price(price)
                    .priceCop(priceCop)
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
            log.info("Plan {} creado con price={} USD y priceCop={} COP (id={})", code, price, priceCop, plan.getId());
        }
    }
}
