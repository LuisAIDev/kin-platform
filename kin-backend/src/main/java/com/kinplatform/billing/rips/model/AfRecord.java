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
@Table(name = "rips_af_records")
public class AfRecord {

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

    @Column(name = "nit_eps", length = 20)
    private String nitEps;

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin")
    private LocalDate periodoFin;

    @Column(name = "numero_factura", length = 20)
    private String numeroFactura;

    @Column(name = "fecha_factura")
    private LocalDate fechaFactura;

    @Column(name = "valor_total", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "tipo_factura", length = 2)
    private String tipoFactura;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public static AfRecord fromContract(EpsContract contract,
                                        LocalDate periodStart, LocalDate periodEnd,
                                        UUID batchId, int sequence, BigDecimal valorTotal) {
        return AfRecord.builder()
                .batchId(batchId)
                .sequenceNumber(sequence)
                .sourceEntityType("CONTRACT")
                .sourceEntityId(contract.getId())
                .codigoPrestador(contract.getOrganizationId().toString().substring(0, 10))
                .nitEps(contract.getEpsNit())
                .periodoInicio(periodStart)
                .periodoFin(periodEnd)
                .numeroFactura(contract.getDianPrefix() + String.format("%010d", contract.getDianCurrentSequence() + 1))
                .fechaFactura(LocalDate.now())
                .valorTotal(valorTotal)
                .tipoFactura("01")
                .ripsLineData(buildJsonLine(contract, periodStart, periodEnd, valorTotal))
                .build();
    }

    private static String buildJsonLine(EpsContract contract,
                                        LocalDate periodStart, LocalDate periodEnd, BigDecimal valorTotal) {
        return String.format("""
            {"codigo_prestador":"%s","nit_eps":"%s","periodo_inicio":"%s","periodo_fin":"%s","numero_factura":"%s","fecha_factura":"%s","valor_total":%s,"tipo_factura":"01"}""",
            contract.getOrganizationId().toString().substring(0, 10),
            contract.getEpsNit(),
            periodStart,
            periodEnd,
            contract.getDianPrefix() + String.format("%010d", contract.getDianCurrentSequence() + 1),
            LocalDate.now(),
            valorTotal
        );
    }
}