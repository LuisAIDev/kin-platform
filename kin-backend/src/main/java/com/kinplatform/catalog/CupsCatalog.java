package com.kinplatform.catalog;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cups_catalog")
public class CupsCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cups_code", nullable = false, unique = true, length = 20)
    private String cupsCode;

    @Column(name = "cups_description", nullable = false, length = 500)
    private String cupsDescription;

    @Column(name = "cups_category", nullable = false, length = 50)
    private String cupsCategory;

    @Column(name = "cups_group", length = 100)
    private String cupsGroup;

    @Column(name = "cups_version", nullable = false, length = 10)
    @Builder.Default
    private String cupsVersion = "2024";

    @Column(name = "reference_price_cop", precision = 14, scale = 2)
    private BigDecimal referencePriceCop;

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
