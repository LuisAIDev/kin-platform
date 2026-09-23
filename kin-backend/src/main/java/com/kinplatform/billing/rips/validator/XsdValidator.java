package com.kinplatform.billing.rips.validator;

import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class XsdValidator {

    private final RipsBatchRepository batchRepository;
    private final RipsRecordRepository recordRepository;

    public ValidationResult validate(RipsBatch batch) {
        List<String> errors = new ArrayList<>();

        String schemaPath = "/schemas/minsalud/2024/rips_" + batch.getRipsType().name().toLowerCase() + ".xsd";
        Schema schema = loadSchema(schemaPath);

        if (schema == null) {
            errors.add("Schema XSD no encontrado: " + schemaPath);
            return ValidationResult.invalid(errors);
        }

        List<RipsRecord> records = recordRepository.findByBatchIdOrderBySequenceNumber(batch.getId());

        Validator validator = schema.newValidator();
        int row = 0;
        for (RipsRecord record : records) {
            row++;
            try {
                String xmlLine = convertRecordToXml(record, batch.getRipsType());
                validator.validate(new StreamSource(new StringReader(xmlLine)));
            } catch (Exception e) {
                errors.add("Fila " + row + ": " + e.getMessage());
                record.setValidationStatus("INVALID");
                record.setValidationError(e.getMessage());
                recordRepository.save(record);
            }
        }

        if (!errors.isEmpty()) {
            recordRepository.saveAll(records);
            return ValidationResult.invalid(errors);
        }

        records.forEach(r -> r.setValidationStatus("VALID"));
        recordRepository.saveAll(records);

        return ValidationResult.valid();
    }

    private Schema loadSchema(String path) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            var resource = getClass().getResourceAsStream(path);
            if (resource == null) return null;
            return factory.newSchema(new StreamSource(resource));
        } catch (Exception e) {
            log.error("Error cargando schema {}: {}", path, e.getMessage());
            return null;
        }
    }

    private String convertRecordToXml(RipsRecord record, RipsBatch.RipsType type) {
        return "<record/>";
    }
}