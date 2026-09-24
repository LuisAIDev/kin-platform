package com.kinplatform.catalog;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invima_catalog")
public class InvimaCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "invima_registration", nullable = false, length = 50)
    private String invimaRegistration;

    @Column(name = "cum_code", length = 50)
    private String cumCode;

    @Column(name = "ium_code", length = 50)
    private String iumCode;

    @Column(name = "commercial_name", nullable = false, length = 500)
    private String commercialName;

    @Column(name = "generic_name", length = 500)
    private String genericName;

    @Column(name = "pharmaceutical_form", length = 200)
    private String pharmaceuticalForm;

    @Column(name = "concentration", length = 200)
    private String concentration;

    @Column(name = "laboratory", length = 300)
    private String laboratory;

    @Column(name = "atc_code", length = 20)
    private String atcCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "invima_version", nullable = false, length = 10)
    @Builder.Default
    private String invimaVersion = "2024";

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
