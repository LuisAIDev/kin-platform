package com.kinplatform.billing.rips.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.rips.generator.RipsGenerationContext;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import com.kinplatform.billing.rips.model.SisproModalidadPagoRepository;
import com.kinplatform.billing.rips.model.SisproCoberturaPlanRepository;
import com.kinplatform.billing.rips.model.SisproConceptoRecaudoRepository;
import com.kinplatform.billing.rips.model.SisproTipoIdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RipsValidatorTest {

    @Mock
    private RipsRecordRepository recordRepository;

    @Mock
    private SisproModalidadPagoRepository modalidadPagoRepository;

    @Mock
    private SisproCoberturaPlanRepository coberturaPlanRepository;

    @Mock
    private SisproConceptoRecaudoRepository conceptoRecaudoRepository;

    @Mock
    private SisproTipoIdRepository tipoIdRepository;

    private RipsValidator validator;

    private final UUID batchId = UUID.randomUUID();

    private static final String VALID_AF_JSON = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "nitEps": "890900123",
            "periodoInicio": "2026-01-01",
            "periodoFin": "2026-01-31",
            "numeroFactura": "FEV0000000001",
            "valorTotal": "5000000.00",
            "tipoFactura": "1",
            "numDocumento": "123456789"
        }
        """;

    private static final String VALID_AC_JSON = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "numFactura": "FEV0000000001",
            "codigoConsulta": "890201",
            "tipoDocumento": "CC",
            "numDocumento": "123456789",
            "fechaConsulta": "2026-01-15",
            "codigoDiagnosticoPrincipal": "Z00",
            "valorConsulta": "50000.00"
        }
        """;

    private static final String VALID_US_JSON = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "tipoDocumento": "CC",
            "numDocumento": "123456789",
            "primerNombre": "Juan",
            "segundoNombre": "Carlos",
            "primerApellido": "Perez",
            "segundoApellido": "Gomez",
            "fechaNacimiento": "1990-01-15",
            "sexo": "M"
        }
        """;

    private static final String INVALID_MISSING_COD_PRESTADOR = """
        {
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31"
        }
        """;

    private static final String INVALID_NEGATIVE_MONETARY = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "valorConsulta": "-50000.00"
        }
        """;

    private static final String INVALID_DATE_FORMAT = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "01/01/2026",
            "fechaFinPeriodo": "2026-01-31"
        }
        """;

    private static final String INVALID_CONTRATO_POLIZA_BOTH = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "numeroContrato": "CT-001",
            "numeroPoliza": "POL-001"
        }
        """;

    private static final String INVALID_TIPO_OPERACION = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "tipoOperacion": "INVALIDO"
        }
        """;

    private static final String INVALID_MONETARY_FORMAT = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "valorConsulta": "$50,000.00"
        }
        """;

    private static final String INVALID_US_MISSING_FIELDS = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "numDocumento": "123456789",
            "primerApellido": "Perez",
            "fechaNacimiento": "1990-01-15",
            "sexo": "M"
        }
        """;

    private static final String INVALID_MODALIDAD_PAGO = """
        {
            "codPrestador": "12345",
            "modalidadPago": "99",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31"
        }
        """;

    private static final String INVALID_COBERTURA_PLAN = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "99",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31"
        }
        """;

    private static final String INVALID_TIPO_DOCUMENTO = """
        {
            "codPrestador": "12345",
            "modalidadPago": "01",
            "coberturaPlanBeneficios": "01",
            "fechaInicioPeriodo": "2026-01-01",
            "fechaFinPeriodo": "2026-01-31",
            "tipoDocumento": "XX",
            "numDocumento": "123456789",
            "primerNombre": "Juan",
            "primerApellido": "Perez",
            "fechaNacimiento": "1990-01-15",
            "sexo": "M"
        }
        """;

    @BeforeEach
    void setUp() {
        when(modalidadPagoRepository.existsByCodigoAndActivoTrue("01")).thenReturn(true);
        when(modalidadPagoRepository.existsByCodigoAndActivoTrue("02")).thenReturn(true);
        when(modalidadPagoRepository.existsByCodigoAndActivoTrue("99")).thenReturn(false);

        when(coberturaPlanRepository.existsByCodigoAndActivoTrue("01")).thenReturn(true);
        when(coberturaPlanRepository.existsByCodigoAndActivoTrue("POS")).thenReturn(false);
        when(coberturaPlanRepository.existsByCodigoAndActivoTrue("99")).thenReturn(false);

        when(tipoIdRepository.existsByCodigoAndActivoTrue("CC")).thenReturn(true);
        when(tipoIdRepository.existsByCodigoAndActivoTrue("TI")).thenReturn(true);
        when(tipoIdRepository.existsByCodigoAndActivoTrue("XX")).thenReturn(false);

        validator = new RipsValidator(recordRepository, new ObjectMapper(), modalidadPagoRepository, coberturaPlanRepository, conceptoRecaudoRepository, tipoIdRepository);
    }

    private RipsBatch batch(RipsBatch.RipsType type) {
        return RipsBatch.builder().id(batchId).ripsType(type).build();
    }

    private RipsRecord record(int seq, String json) {
        return RipsRecord.builder().id(UUID.randomUUID()).sequenceNumber(seq).ripsLineData(json).build();
    }

    @Test
    void validate_validAfRecord_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AF_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AF), mock(RipsGenerationContext.class));

        assertTrue(result.isValid(), () -> "errores: " + result.getErrors());
    }

    @Test
    void validate_validAcRecord_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertTrue(result.isValid(), () -> "errores: " + result.getErrors());
    }

    @Test
    void validate_validUsRecord_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_US_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.US), mock(RipsGenerationContext.class));

        assertTrue(result.isValid(), () -> "errores: " + result.getErrors());
    }

    @Test
    void validate_missingCodPrestador_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_MISSING_COD_PRESTADOR)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("CODIGO_PRESTADOR es obligatorio"));
    }

    @Test
    void validate_negativeMonetary_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_NEGATIVE_MONETARY)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("no puede ser negativo") || e.contains("formato monetario inválido")));
    }

    @Test
    void validate_invalidDateFormat_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_DATE_FORMAT)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("formato inválido"));
    }

    @Test
    void validate_contratoAndPolizaBothPresent_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_CONTRATO_POLIZA_BOTH)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("exclusividad"));
    }

    @Test
    void validate_invalidTipoOperacion_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_TIPO_OPERACION)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("TIPO_OPERACION inválido"));
    }

    @Test
    void validate_invalidMonetaryFormat_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_MONETARY_FORMAT)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("formato monetario inválido"));
    }

    @Test
    void validate_missingUsFields_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_US_MISSING_FIELDS)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.US), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("PRIMER_NOMBRE es obligatorio")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("TIPO_DOCUMENTO es obligatorio")));
    }

    @Test
    void validate_multipleRecords_aggregatesErrors() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON), record(2, INVALID_MISSING_COD_PRESTADOR)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 1);
    }

    @Test
    void validate_emptyRecords_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of());

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertTrue(result.isValid());
    }

    @Test
    void validate_crossRecordInconsistentModalidad_fails() {
        String af1 = VALID_AF_JSON;
        String af2 = VALID_AF_JSON.replace("\"modalidadPago\": \"01\"", "\"modalidadPago\": \"02\"");

        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, af1), record(2, af2)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AF), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("MODALIDAD_PAGO inconsistente")));
    }

    @Test
    void validate_crossRecordInconsistentCobertura_fails() {
        String af1 = VALID_AF_JSON;
        String af2 = VALID_AF_JSON.replace("\"coberturaPlanBeneficios\": \"01\"", "\"coberturaPlanBeneficios\": \"POS\"");

        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, af1), record(2, af2)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AF), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("COBERTURA_PLAN_BENEFICIOS inconsistente")));
    }

    @Test
    void validate_invalidModalidadPago_returnsError() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_MODALIDAD_PAGO)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("no existe en tabla de referencia SISPRO"));
    }

    @Test
    void validate_invalidCoberturaPlan_returnsError() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_COBERTURA_PLAN)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("no existe en tabla de referencia SISPRO"));
    }

    @Test
    void validate_invalidTipoDocumento_returnsError() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_TIPO_DOCUMENTO)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.US), mock(RipsGenerationContext.class));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("no existe en tabla de referencia SISPRO"));
    }

    @Test
    void validate_validModalidadPago_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertTrue(result.isValid(), () -> "errores: " + result.getErrors());
    }

    @Test
    void validate_validCobertura_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC), mock(RipsGenerationContext.class));

        assertTrue(result.isValid(), () -> "errores: " + result.getErrors());
    }
}