package com.kinplatform.billing.rips.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
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
class XsdValidatorTest {

    @Mock
    private RipsRecordRepository recordRepository;

    private XsdValidator validator;

    private final UUID batchId = UUID.randomUUID();

    private static final String VALID_AC_JSON = "{"
            + "\"codigoPrestador\":\"123\","
            + "\"numFactura\":\"FEV0000000001\","
            + "\"codigoConsulta\":\"890201\","
            + "\"tipoDocumento\":\"CC\","
            + "\"numDocumento\":\"123456\","
            + "\"fechaConsulta\":\"2026-01-15\","
            + "\"codigoDiagnosticoPrincipal\":\"Z00\","
            + "\"valorConsulta\":50000}";

    private static final String INVALID_AC_JSON = "{"
            + "\"numFactura\":\"FEV0000000001\","
            + "\"codigoConsulta\":\"890201\","
            + "\"tipoDocumento\":\"CC\","
            + "\"numDocumento\":\"123456\","
            + "\"fechaConsulta\":\"2026-01-15\","
            + "\"codigoDiagnosticoPrincipal\":\"Z00\","
            + "\"valorConsulta\":50000}";

    @BeforeEach
    void setUp() {
        validator = new XsdValidator(recordRepository, new RipsJsonToXmlMapper(new ObjectMapper()));
    }

    private RipsBatch batch(RipsBatch.RipsType type) {
        return RipsBatch.builder().id(batchId).ripsType(type).build();
    }

    private RipsRecord record(int seq, String json) {
        return RipsRecord.builder().id(UUID.randomUUID()).sequenceNumber(seq).ripsLineData(json).build();
    }

    @Test
    void validate_validAcRecord_withPlaceholderSchema_passes() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC));

        assertTrue(result.isValid(), () -> "errores: " + result.getErrors());
    }

    @Test
    void validate_invalidAcRecord_missingField_fails() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, INVALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC));

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void validate_multipleRecords_aggregatesErrors() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON), record(2, INVALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AC));

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void validate_missingSchema_returnsError() {
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId))
                .thenReturn(List.of(record(1, VALID_AC_JSON)));

        ValidationResult result = validator.validate(batch(RipsBatch.RipsType.AT));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Schema XSD no encontrado"));
    }

    @Test
    void getSchema_loadsPlaceholderForAc() {
        assertNotNull(validator.getSchema(RipsBatch.RipsType.AC));
    }
}
