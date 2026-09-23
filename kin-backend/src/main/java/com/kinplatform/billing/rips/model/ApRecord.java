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
@Table(name = "rips_ap_records")
public class ApRecord {

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

    @Column(name = "codigo_procedimiento", length = 20)
    private String codigoProcedimiento;

    @Column(name = "fecha_procedimiento")
    private LocalDate fechaProcedimiento;

    @Column(name = "codigo_diagnostico", length = 20)
    private String codigoDiagnostico;

    @Column(name = "valor_procedimiento", precision = 14, scale = 2)
    private BigDecimal valorProcedimiento;

    @Column(name = "ambito_realizacion", length = 1)
    private String ambitoRealizacion;

    @Column(name = "finalidad_procedimiento", length = 1)
    private String finalidadProcedimiento;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}