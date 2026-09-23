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
public class ApRecord {

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
    private String codigoProcedimiento;
    private LocalDate fechaProcedimiento;
    private String codigoDiagnostico;
    private BigDecimal valorProcedimiento;
    private String ambitoRealizacion;
    private String finalidadProcedimiento;
}
