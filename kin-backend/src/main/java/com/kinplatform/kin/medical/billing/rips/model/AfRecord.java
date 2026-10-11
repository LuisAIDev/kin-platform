package com.kinplatform.kin.medical.billing.rips.model;

import com.kinplatform.kin.medical.billing.contract.EpsContract;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfRecord {

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
    private String nitEps;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String numeroFactura;
    private LocalDate fechaFactura;
    private BigDecimal valorTotal;
    private String tipoFactura;

    @Pattern(regexp = "SS-CUFE|SS-CUDE|SS-POS|SS-SNum|SS-Recaudo|SS-Reporte|SS-SinAporte", message = "tipoOperacion debe ser válido")
    private String tipoOperacion;

    @NotBlank(message = "modalidadPago es obligatorio")
    private String modalidadPago;

    @NotBlank(message = "coberturaPlanBeneficios es obligatorio")
    private String coberturaPlanBeneficios;

    private String numeroContrato;

    private String numeroPoliza;

    @DecimalMin(value = "0.0", message = "pagosCompartidos no puede ser negativo")
    private BigDecimal pagosCompartidos;

    @NotNull(message = "fechaInicioPeriodo es obligatorio")
    private LocalDate fechaInicioPeriodo;

    @NotNull(message = "fechaFinPeriodo es obligatorio")
    private LocalDate fechaFinPeriodo;

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


