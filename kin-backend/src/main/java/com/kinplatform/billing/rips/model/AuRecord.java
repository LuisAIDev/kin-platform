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
        String codigoPrestador = contract.getOrganizationId().toString();
        if (codigoPrestador.length() > 12) {
            codigoPrestador = codigoPrestador.substring(0, 12);
        }
        String numFactura = contract.getDianPrefix() + String.format("%010d", contract.getDianCurrentSequence() + 1);
        LocalDate fecha = LocalDate.parse(fechaUrgencia);

        return AuRecord.builder()
                .sourceEntityType("ENCOUNTER")
                .sourceEntityId(resolveSourceId(encounter))
                .codigoPrestador(codigoPrestador)
                .numFactura(numFactura)
                .fechaUrgencia(fecha)
                .motivoUrgencia(motivoUrgencia)
                .codigoDiagnosticoSalida(codigoDiagnosticoSalida)
                .destinoUsuario(destinoUsuario)
                .estadoUsuario(estadoUsuario)
                .valorUrgencia(valorUrgencia)
                .ripsLineData(buildJsonLine(codigoPrestador, numFactura, tipoDocumento, numeroDocumento,
                        fecha, motivoUrgencia, codigoDiagnosticoSalida, destinoUsuario, estadoUsuario, valorUrgencia))
                .build();
    }

    private static UUID resolveSourceId(Object encounter) {
        try {
            Object id = encounter.getClass().getMethod("getId").invoke(encounter);
            if (id instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
            // fall through to random id
        }
        return UUID.randomUUID();
    }

    private static String buildJsonLine(String codigoPrestador, String numFactura, String tipoDocumento,
            String numeroDocumento, LocalDate fecha, String motivoUrgencia, String codigoDiagnosticoSalida,
            String destinoUsuario, String estadoUsuario, java.math.BigDecimal valorUrgencia) {
        return String.format(
                "{\"codigo_prestador\":\"%s\",\"num_factura\":\"%s\",\"tipo_documento\":\"%s\",\"numero_documento\":\"%s\",\"fecha_urgencia\":\"%s\",\"motivo_urgencia\":\"%s\",\"codigo_diagnostico_salida\":\"%s\",\"destino_usuario\":\"%s\",\"estado_usuario\":\"%s\",\"valor_urgencia\":%s}",
                codigoPrestador, numFactura, tipoDocumento, numeroDocumento, fecha, motivoUrgencia,
                codigoDiagnosticoSalida, destinoUsuario, estadoUsuario, valorUrgencia);
    }
}