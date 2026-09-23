package com.kinplatform.billing.rips.model;

import com.kinplatform.billing.contract.EpsContract;
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