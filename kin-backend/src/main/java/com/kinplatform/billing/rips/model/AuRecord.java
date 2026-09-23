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
@Table(name = "rips_au_records")
public class AuRecord {

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

    @Column(name = "fecha_urgencia")
    private LocalDate fechaUrgencia;

    @Column(name = "motivo_urgencia", length = 20)
    private String motivoUrgencia;

    @Column(name = "codigo_diagnostico_salida", length = 20)
    private String codigoDiagnosticoSalida;

    @Column(name = "destino_usuario", length = 1)
    private String destinoUsuario;

    @Column(name = "estado_usuario", length = 1)
    private String estadoUsuario;

    @Column(name = "valor_urgencia", precision = 14, scale = 2)
    private BigDecimal valorUrgencia;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    // Explicit factory method for cross-compilation-unit visibility (Lombok @Builder may not work)
    public static AuRecord fromEncounter(
            Object encounter,
            com.kinplatform.billing.contract.EpsContract contract,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd,
            String tipoDocumento,
            String numeroDocumento,
            String fechaUrgencia,
            String motivoUrgencia,
            String codigoDiagnosticoSalida,
            String destinoUsuario,
            String estadoUsuario,
            String cupsCode,
            java.math.BigDecimal valorUrgencia) {
        AuRecord record = new AuRecord();
        record.setSourceEntityType("ENCOUNTER");
        record.setSourceEntityId(java.util.UUID.randomUUID()); // placeholder
        record.setBatchId(java.util.UUID.randomUUID()); // placeholder
        record.setSequenceNumber(1);
        record.setCreatedAt(OffsetDateTime.now());
        record.setCodigoPrestador(contract.getOrganizationId().toString().substring(0, Math.min(12, contract.getOrganizationId().toString().length())));
        record.setNumFactura(contract.getDianPrefix() + String.format("%010d", contract.getDianCurrentSequence() + 1));
        record.setFechaUrgencia(LocalDate.parse(fechaUrgencia));
        record.setMotivoUrgencia(motivoUrgencia);
        record.setCodigoDiagnosticoSalida(codigoDiagnosticoSalida);
        record.setDestinoUsuario(destinoUsuario);
        record.setEstadoUsuario(estadoUsuario);
        record.setValorUrgencia(valorUrgencia);
        return record;
    }
}