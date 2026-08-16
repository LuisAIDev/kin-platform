package com.kinplatform.pricing;

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
     */
    @Column(length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

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
