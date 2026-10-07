package com.kinplatform.common.pricing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pricing_plans")
public class PricingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Identificador comercial estable del plan: {@code FREE}, {@code STANDARD}
     * o {@code PREMIUM}. No depende del nombre (que es solo cosmético).
     * Único dentro de cada {@link ProductVertical} (compuesto con vertical).
     */
    @Column(length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "vertical", nullable = false, length = 30)
    @Builder.Default
    private ProductVertical vertical = ProductVertical.EMPRESAS;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Precio en pesos colombianos (COP) para la pasarela Wompi. Wompi solo
     * opera en COP, mientras que {@link #price} está en USD para Stripe. Si es
     * {@code null}, el plan no puede pagarse con Wompi (el checkout falla con
     * un error explícito). No se inventa un valor: lo define el negocio.
     */
    @Column(name = "price_cop", precision = 12, scale = 2)
    private BigDecimal priceCop;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private String features;

    /**
     * Máximo de proyectos COMPLETADOS por período (mensual). {@code null} =
     * ilimitado (PREMIUM). En esta arquitectura la semántica es
     * "proyectos completados por período", no proyectos existentes.
     */
    @Column(name = "max_projects")
    private Integer maxProjects;

    @Column(name = "messages_per_month")
    private Integer messagesPerMonth;

    /** Presupuesto de IA (USD) por período. Fuente de verdad del gate de costo. */
    @Column(name = "ai_budget_usd", precision = 10, scale = 2)
    private BigDecimal aiBudgetUsd;

    /**
     * Máximo de triajes por mes (KIN Salud Personal).
     * {@code null} = ilimitado (plan pago PERSONAL_PLUS).
     */
    @Column(name = "max_triages_per_month")
    private Integer maxTriagesPerMonth;

    /**
     * Días de prueba gratuita (KIN Salud Profesional / Personal+).
     * {@code null} = sin trial (plan pago PROFESSIONAL).
     * {@code 0} = sin trial (FREE, planes de pago sin trial).
     * {@code >0} = días de trial (ej. 14 para Personal+).
     */
    @Column(name = "trial_days")
    private Integer trialDays;

    /**
     * Máximo de almacenamiento en MB (KIN Salud Personal).
     * {@code null} = ilimitado (planes PROFESSIONAL, PREMIUM).
     * {@code 0} = sin almacenamiento.
     * {@code >0} = MB permitidos (ej. 50 para FREE, 5000 para PERSONAL_PLUS).
     */
    @Column(name = "max_storage_mb")
    private Integer maxStorageMb;

    /**
     * Máximo de pacientes propios (KIN Salud Profesional pago).
     * {@code null} = ilimitado.
     */
    @Column(name = "max_patients")
    private Integer maxPatients;

    @Column(name = "advanced_ai", nullable = false)
    @Builder.Default
    private Boolean advancedAI = false;

    @Column(name = "pdf_export", nullable = false)
    @Builder.Default
    private Boolean pdfExport = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_level", nullable = false, length = 20)
    @Builder.Default
    private SupportLevel supportLevel = SupportLevel.BASIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "viability_scoring_detail", nullable = false, length = 20)
    @Builder.Default
    private ViabilityScoringDetail viabilityScoringDetail = ViabilityScoringDetail.BASIC;

    @Column(name = "triage_sharing", nullable = false)
    @Builder.Default
    private Boolean triageSharing = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

