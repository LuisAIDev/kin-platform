package com.kinplatform.billing.rips.validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.rips.generator.RipsGenerationContext;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class RipsValidator {

    private final RipsRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> VALID_TIPO_OPERACION = Set.of(
        "SS-CUFE", "SS-CUDE", "SS-POS", "SS-SNum", "SS-Recaudo", "SS-Reporte", "SS-SinAporte"
    );

    private static final Set<String> ACTIVA_METODO = Set.of(
        "SS-CUFE", "SS-CUDE", "SS-POS", "SS-SNum"
    );

    private static final Pattern MONETARIO_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ValidationResult validate(RipsBatch batch, RipsGenerationContext context) {
        List<String> errors = new ArrayList<>();

        List<RipsRecord> records = recordRepository.findByBatchIdOrderBySequenceNumber(batch.getId());
        if (records.isEmpty()) {
            return ValidationResult.valid();
        }

        for (int i = 0; i < records.size(); i++) {
            RipsRecord record = records.get(i);
            int row = i + 1;
            try {
                Map<String, Object> data = objectMapper.readValue(
                    record.getRipsLineData(), new TypeReference<Map<String, Object>>() {});

                validateRecord(batch.getRipsType(), data, row, errors);

            } catch (Exception e) {
                errors.add("Fila " + row + ": Error parseando JSON - " + e.getMessage());
            }
        }

        validateCrossRecordRules(records, batch.getRipsType(), errors);

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private void validateRecord(RipsBatch.RipsType type, Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        validateMandatoryFields(data, row, errors);
        validateMonetaryFields(data, row, errors);
        validateDateFields(data, row, errors);
        validateExclusivityContratoPoliza(data, row, errors);
        validateTipoOperacion(data, row, errors);

        switch (type) {
            case AF -> validateAFSpecific(data, row, errors);
            case AC, AP, AU, AT -> validateDetailSpecific(data, row, errors);
            case US -> validateUSSpecific(data, row, errors);
        }
    }

    private void validateMandatoryFields(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String codPrestador = getString(data, "codPrestador");
        if (isBlank(codPrestador)) {
            errors.add(prefix + "CODIGO_PRESTADOR es obligatorio");
        }

        String modalidadPago = getString(data, "modalidadPago");
        if (isBlank(modalidadPago)) {
            errors.add(prefix + "MODALIDAD_PAGO es obligatorio");
        } else if (!isValidModalidadPago(modalidadPago)) {
            errors.add(prefix + "MODALIDAD_PAGO inválido: " + modalidadPago);
        }

        String coberturaPlan = getString(data, "coberturaPlanBeneficios");
        if (isBlank(coberturaPlan)) {
            errors.add(prefix + "COBERTURA_PLAN_BENEFICIOS es obligatorio");
        } else if (!isValidCoberturaPlan(coberturaPlan)) {
            errors.add(prefix + "COBERTURA_PLAN_BENEFICIOS inválido: " + coberturaPlan);
        }

        String fechaInicio = getString(data, "fechaInicioPeriodo");
        if (isBlank(fechaInicio)) {
            errors.add(prefix + "FECHA_INICIO_PERIODO es obligatorio");
        } else if (!isValidDate(fechaInicio)) {
            errors.add(prefix + "FECHA_INICIO_PERIODO formato inválido (debe ser AAAA-MM-DD): " + fechaInicio);
        }

        String fechaFin = getString(data, "fechaFinPeriodo");
        if (isBlank(fechaFin)) {
            errors.add(prefix + "FECHA_FIN_PERIODO es obligatorio");
        } else if (!isValidDate(fechaFin)) {
            errors.add(prefix + "FECHA_FIN_PERIODO formato inválido (debe ser AAAA-MM-DD): " + fechaFin);
        }
    }

    private void validateMonetaryFields(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String[] monetaryFields = {"copago", "cuotaModeradora", "pagosCompartidos", "anticipo", "valorConsulta", "valorProcedimiento", "valorUrgencia", "valorUnitario", "valorTotal", "valorTotalFactura"};

        for (String field : monetaryFields) {
            Object value = data.get(field);
            if (value != null) {
                String strValue = String.valueOf(value);
                if (!isValidMonetary(strValue)) {
                    errors.add(prefix + field.toUpperCase() + " formato monetario inválido (sin símbolos, sin separadores de miles, decimal con punto): " + strValue);
                } else {
                    try {
                        BigDecimal decimal = new BigDecimal(strValue);
                        if (decimal.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(prefix + field.toUpperCase() + " no puede ser negativo: " + strValue);
                        }
                    } catch (NumberFormatException e) {
                        errors.add(prefix + field.toUpperCase() + " no es un número válido: " + strValue);
                    }
                }
            }
        }
    }

    private void validateDateFields(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String[] dateFields = {"fechaConsulta", "fechaProcedimiento", "fechaUrgencia", "fechaServicio", "fechaNacimiento", "fechaInicioPeriodo", "fechaFinPeriodo"};

        for (String field : dateFields) {
            Object value = data.get(field);
            if (value != null) {
                String strValue = String.valueOf(value);
                if (!isValidDate(strValue)) {
                    errors.add(prefix + field.toUpperCase() + " formato inválido (debe ser AAAA-MM-DD): " + strValue);
                }
            }
        }
    }

    private void validateExclusivityContratoPoliza(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String numContrato = getString(data, "numeroContrato");
        String numPoliza = getString(data, "numeroPoliza");

        if (!isBlank(numContrato) && !isBlank(numPoliza)) {
            errors.add(prefix + "Regla de exclusividad: NUMERO_CONTRATO y NUMERO_POLIZA no pueden tener valor simultáneamente");
        }
    }

    private void validateTipoOperacion(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String tipoOperacion = getString(data, "tipoOperacion");
        if (!isBlank(tipoOperacion)) {
            if (!VALID_TIPO_OPERACION.contains(tipoOperacion)) {
                errors.add(prefix + "TIPO_OPERACION inválido: " + tipoOperacion + ". Válidos: " + VALID_TIPO_OPERACION);
            }
        }
    }

    private void validateAFSpecific(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String nitEps = getString(data, "nitEps");
        if (isBlank(nitEps)) {
            errors.add(prefix + "NIT_EPS es obligatorio en AF");
        }

        String periodoInicio = getString(data, "periodoInicio");
        String periodoFin = getString(data, "periodoFin");
        if (isBlank(periodoInicio) || isBlank(periodoFin)) {
            errors.add(prefix + "PERIODO_INICIO y PERIODO_FIN son obligatorios en AF");
        }

        String numeroFactura = getString(data, "numeroFactura");
        if (isBlank(numeroFactura)) {
            errors.add(prefix + "NUMERO_FACTURA es obligatorio en AF");
        }

        String valorTotal = getString(data, "valorTotal");
        if (isBlank(valorTotal)) {
            errors.add(prefix + "VALOR_TOTAL es obligatorio en AF");
        } else if (!isValidMonetary(valorTotal)) {
            errors.add(prefix + "VALOR_TOTAL formato inválido: " + valorTotal);
        }

        String tipoFactura = getString(data, "tipoFactura");
        if (isBlank(tipoFactura)) {
            errors.add(prefix + "TIPO_FACTURA es obligatorio en AF");
        }
    }

    private void validateDetailSpecific(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String numFactura = getString(data, "numFactura");
        if (isBlank(numFactura)) {
            errors.add(prefix + "NUM_FACTURA es obligatorio en detalle");
        }
    }

    private void validateUSSpecific(Map<String, Object> data, int row, List<String> errors) {
        String prefix = "Fila " + row + ": ";

        String tipoDoc = getString(data, "tipoDocumento");
        if (isBlank(tipoDoc)) {
            errors.add(prefix + "TIPO_DOCUMENTO es obligatorio en US");
        } else if (!isValidTipoIdentificacion(tipoDoc)) {
            errors.add(prefix + "TIPO_DOCUMENTO inválido (debe ser código SISPRO): " + tipoDoc);
        }

        String numDoc = getString(data, "numDocumento");
        if (isBlank(numDoc)) {
            errors.add(prefix + "NUM_DOCUMENTO es obligatorio en US");
        }

        String primerNombre = getString(data, "primerNombre");
        if (isBlank(primerNombre)) {
            errors.add(prefix + "PRIMER_NOMBRE es obligatorio en US");
        }

        String primerApellido = getString(data, "primerApellido");
        if (isBlank(primerApellido)) {
            errors.add(prefix + "PRIMER_APELLIDO es obligatorio en US");
        }

        String fechaNac = getString(data, "fechaNacimiento");
        if (isBlank(fechaNac)) {
            errors.add(prefix + "FECHA_NACIMIENTO es obligatorio en US");
        } else if (!isValidDate(fechaNac)) {
            errors.add(prefix + "FECHA_NACIMIENTO formato inválido: " + fechaNac);
        }

        String sexo = getString(data, "sexo");
        if (isBlank(sexo)) {
            errors.add(prefix + "SEXO es obligatorio en US");
        } else if (!Set.of("M", "F").contains(sexo)) {
            errors.add(prefix + "SEXO debe ser M o F: " + sexo);
        }
    }

    private void validateCrossRecordRules(List<RipsRecord> records, RipsBatch.RipsType type, List<String> errors) {
        if (type != RipsBatch.RipsType.AF) {
            return;
        }

        Map<String, String> modalidadPorUsuario = new HashMap<>();
        Map<String, String> coberturaPorUsuario = new HashMap<>();

        for (int i = 0; i < records.size(); i++) {
            try {
                RipsRecord record = records.get(i);
                Map<String, Object> data = objectMapper.readValue(
                    record.getRipsLineData(), new TypeReference<Map<String, Object>>() {});

                String numDoc = getString(data, "numDocumento");
                String modalidad = getString(data, "modalidadPago");
                String cobertura = getString(data, "coberturaPlanBeneficios");

                if (!isBlank(numDoc)) {
                    if (modalidadPorUsuario.containsKey(numDoc) && !modalidadPorUsuario.get(numDoc).equals(modalidad)) {
                        errors.add("Fila " + (i + 1) + ": Usuario " + numDoc + " tiene MODALIDAD_PAGO inconsistente (" + modalidadPorUsuario.get(numDoc) + " vs " + modalidad + ")");
                    }
                    modalidadPorUsuario.put(numDoc, modalidad);

                    if (coberturaPorUsuario.containsKey(numDoc) && !coberturaPorUsuario.get(numDoc).equals(cobertura)) {
                        errors.add("Fila " + (i + 1) + ": Usuario " + numDoc + " tiene COBERTURA_PLAN_BENEFICIOS inconsistente (" + coberturaPorUsuario.get(numDoc) + " vs " + cobertura + ")");
                    }
                    coberturaPorUsuario.put(numDoc, cobertura);
                }

            } catch (Exception e) {
                errors.add("Fila " + (i + 1) + ": Error validando reglas transversales - " + e.getMessage());
            }
        }
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isValidMonetary(String value) {
        return MONETARIO_PATTERN.matcher(value).matches();
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value, DATE_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidModalidadPago(String value) {
        return true;
    }

    private boolean isValidCoberturaPlan(String value) {
        return true;
    }

    private boolean isValidTipoIdentificacion(String value) {
        return true;
    }
}