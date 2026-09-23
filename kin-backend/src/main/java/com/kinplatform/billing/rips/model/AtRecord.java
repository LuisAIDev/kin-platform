package com.kinplatform.billing.rips.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rips_at_records")
public class AtRecord {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "source_entity_type", nullable = false, length = 30)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false)
    private UUID sourceEntityId;

    @Column(name = "rips_line_data", nullable = false, columnDefinition = "jsonb")
    private String ripsLineData;

    @Column(name = "validation_status", length = 20)
    private String validationStatus;

    @Column(name = "validation_error", length = 2000)
    private String validationError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "codigo_prestador", length = 12)
    private String codigoPrestador;

    @Column(name = "num_factura", length = 20)
    private String numFactura;

    @Column(name = "tipo_otros_servicios", length = 20)
    private String tipoOtrosServicios;

    @Column(name = "codigo_servicio", length = 20)
    private String codigoServicio;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "cantidad")
    private Integer cantidad;

    @Column(name = "valor_unitario", precision = 14, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "fecha_servicio")
    private LocalDate fechaServicio;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}