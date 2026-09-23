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
@Table(name = "rips_ac_records")
public class AcRecord {

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

    @Column(name = "codigo_consulta", length = 20)
    private String codigoConsulta;

    @Column(name = "tipo_documento", length = 2)
    private String tipoDocumento;

    @Column(name = "num_documento", length = 20)
    private String numDocumento;

    @Column(name = "fecha_consulta")
    private LocalDate fechaConsulta;

    @Column(name = "codigo_diagnostico_principal", length = 20)
    private String codigoDiagnosticoPrincipal;

    @Column(name = "codigo_diagnostico_relacionado", length = 20)
    private String codigoDiagnosticoRelacionado;

    @Column(name = "valor_consulta", precision = 14, scale = 2)
    private BigDecimal valorConsulta;

    @Column(name = "cuota_moderadora", precision = 14, scale = 2)
    private BigDecimal cuotaModeradora;

    @Column(name = "copago", precision = 14, scale = 2)
    private BigDecimal copago;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}