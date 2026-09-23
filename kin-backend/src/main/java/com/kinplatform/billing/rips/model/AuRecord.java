package com.kinplatform.billing.rips.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuRecord {

    private UUID id;
    private UUID batchId;
    private Integer sequenceNumber;
    private String sourceEntityType;
    private UUID sourceEntityId;
    private String ripsLineData;
    private String validationStatus;
    private String validationError;
    private OffsetDateTime createdAt;
    private String codigoPrestador;
    private String numFactura;
    private LocalDate fechaUrgencia;
    private String motivoUrgencia;
    private String codigoDiagnosticoSalida;
    private String destinoUsuario;
    private String estadoUsuario;
    private BigDecimal valorUrgencia;

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
