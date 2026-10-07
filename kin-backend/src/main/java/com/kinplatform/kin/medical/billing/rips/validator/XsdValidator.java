package com.kinplatform.kin.medical.billing.rips.validator;

import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida cada linea RIPS contra el XSD del tipo correspondiente.
 *
 * <p>Los esquemas se cargan desde {@code /schemas/minsalud/2024/rips_<tipo>.xsd}
 * y se cachean en memoria. En tests se usa un XSD placeholder
 * ({@code src/test/resources/schemas/...}); en produccion se usaran los oficiales
 * de MinSalud (TD-CAT-4).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class XsdValidator {

    private static final String SCHEMA_BASE = "/schemas/minsalud/2024/rips_";

    private final RipsRecordRepository recordRepository;
    private final RipsJsonToXmlMapper jsonToXmlMapper;

    private final ConcurrentHashMap<String, Schema> schemaCache = new ConcurrentHashMap<>();

    public ValidationResult validate(RipsBatch batch) {
        List<RipsRecord> records = recordRepository.findByBatchIdOrderBySequenceNumber(batch.getId());
        if (records.isEmpty()) {
            return ValidationResult.valid();
        }

        Schema schema = getSchema(batch.getRipsType());
        if (schema == null) {
            return ValidationResult.invalid(
                    List.of("Schema XSD no encontrado: " + schemaPath(batch.getRipsType())));
        }

        List<String> errors = new ArrayList<>();
        int row = 0;
        for (RipsRecord record : records) {
            row++;
            try {
                String xml = jsonToXmlMapper.toXml(record, batch.getRipsType());
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(new StringReader(xml)));
                record.setValidationStatus("VALID");
            } catch (Exception e) {
                errors.add("Fila " + row + ": " + e.getMessage());
                record.setValidationStatus("INVALID");
                record.setValidationError(e.getMessage());
            }
        }
        recordRepository.saveAll(records);

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    public Schema getSchema(RipsBatch.RipsType type) {
        return schemaCache.computeIfAbsent(type.name(), key -> loadSchema(schemaPath(type)));
    }

    private static String schemaPath(RipsBatch.RipsType type) {
        return SCHEMA_BASE + type.name().toLowerCase() + ".xsd";
    }

    private Schema loadSchema(String path) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            var resource = getClass().getResourceAsStream(path);
            if (resource == null) {
                return null;
            }
            return factory.newSchema(new StreamSource(resource));
        } catch (Exception e) {
            log.error("Error cargando schema {}: {}", path, e.getMessage());
            return null;
        }
    }
}


