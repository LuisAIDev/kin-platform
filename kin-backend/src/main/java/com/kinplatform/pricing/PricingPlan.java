package com.kinplatform.pricing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String features;

    @Column(name = "max_projects")
    private Integer maxProjects;

    @Column(name = "messages_per_month")
    private Integer messagesPerMonth;

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
