package com.kinplatform.billing.rips.model;

import com.kinplatform.billing.contract.EpsContract;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtRecord {

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
    private String tipoOtrosServicios;
    private String codigoServicio;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
    private LocalDate fechaServicio;

    public static AtRecord fromOrder(
            Object order,
            EpsContract contract,
            LocalDate periodStart,
            LocalDate periodEnd,
            String tipoDocumento,
            String numeroDocumento,
            String cupsCode,
            String descripcion,
            int cantidad,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String fechaServicio,
            String tipoOtrosServicios) {
        String codigoPrestador = contract.getOrganizationId().toString();
        if (codigoPrestador.length() > 12) {
            codigoPrestador = codigoPrestador.substring(0, 12);
        }
        String numFactura = contract.getDianPrefix() + String.format("%010d", contract.getDianCurrentSequence() + 1);
        LocalDate fecha = (fechaServicio == null || fechaServicio.isBlank())
                ? LocalDate.now()
                : LocalDate.parse(fechaServicio);

        return AtRecord.builder()
                .sourceEntityType("ORDER")
                .sourceEntityId(resolveSourceId(order))
                .codigoPrestador(codigoPrestador)
                .numFactura(numFactura)
                .tipoOtrosServicios(tipoOtrosServicios)
                .codigoServicio(cupsCode)
                .descripcion(descripcion)
                .cantidad(cantidad)
                .valorUnitario(valorUnitario)
                .valorTotal(valorTotal)
                .fechaServicio(fecha)
                .ripsLineData(buildJsonLine(codigoPrestador, numFactura, tipoDocumento, numeroDocumento,
                        tipoOtrosServicios, cupsCode, descripcion, cantidad, valorUnitario, valorTotal, fecha))
                .build();
    }

    private static UUID resolveSourceId(Object order) {
        try {
            Object id = order.getClass().getMethod("getId").invoke(order);
            if (id instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
            // fall through to random id
        }
        return UUID.randomUUID();
    }

    private static String buildJsonLine(String codigoPrestador, String numFactura, String tipoDocumento,
            String numeroDocumento, String tipoOtrosServicios, String cupsCode, String descripcion,
            int cantidad, BigDecimal valorUnitario, BigDecimal valorTotal, LocalDate fecha) {
        return String.format(
                "{\"codigo_prestador\":\"%s\",\"num_factura\":\"%s\",\"tipo_documento\":\"%s\",\"numero_documento\":\"%s\",\"tipo_otros_servicios\":\"%s\",\"codigo_servicio\":\"%s\",\"descripcion\":\"%s\",\"cantidad\":%d,\"valor_unitario\":%s,\"valor_total\":%s,\"fecha_servicio\":\"%s\"}",
                codigoPrestador, numFactura, tipoDocumento, numeroDocumento, tipoOtrosServicios, cupsCode,
                descripcion, cantidad, valorUnitario, valorTotal, fecha);
    }
}
