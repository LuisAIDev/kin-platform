package com.kinplatform.kin.medical.billing.rips.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsRecord {

    private UUID id;
    private UUID batchId;
    private Integer sequenceNumber;
    private String sourceEntityType;
    private UUID sourceEntityId;
    private String ripsLineData;
    private String validationStatus;
    private String validationError;
    private OffsetDateTime createdAt;
    private String tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String regimen;
    private String codigoMunicipio;
    private String direccion;
    private String telefono;
    private String email;

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
}
