package com.kinplatform.kin.medical.billing.rips.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sispro_modalidad_pago")
public class SisproModalidadPago {

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
