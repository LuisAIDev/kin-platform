package com.kinplatform.kin.medical.billing.rips.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcRecord {

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
    private String codigoConsulta;
    private String tipoDocumento;
    private String numDocumento;
    private LocalDate fechaConsulta;
    private String codigoDiagnosticoPrincipal;
    private String codigoDiagnosticoRelacionado;
    private BigDecimal valorConsulta;
    private BigDecimal cuotaModeradora;
    private BigDecimal copago;
}

