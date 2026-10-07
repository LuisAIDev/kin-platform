package com.kinplatform.kin.medical.billing.rips.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sispro_cobertura_plan")
public class SisproCoberturaPlan {

    @Id
    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (activo == null) activo = true;
        createdAt = OffsetDateTime.now();
    }
}
