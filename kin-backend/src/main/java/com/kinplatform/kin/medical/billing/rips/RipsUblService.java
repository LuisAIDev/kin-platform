package com.kinplatform.kin.medical.billing.rips;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatchRepository;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecordRepository;
import com.kinplatform.kin.medical.billing.rips.ubl.RipsInteroperabilidadPayload;
import com.kinplatform.kin.medical.billing.rips.ubl.UblExtensionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RipsUblService {

    private final RipsBatchRepository batchRepository;
    private final RipsRecordRepository recordRepository;
    private final XmlMapper xmlMapper;

    public RipsUblService(RipsBatchRepository batchRepository, RipsRecordRepository recordRepository) {
        this.batchRepository = batchRepository;
        this.recordRepository = recordRepository;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Transactional(readOnly = true)
    public String generateUblXml(UUID batchId) {
        RipsBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RipsValidationException("Batch no encontrado: " + batchId));

        List<RipsRecord> records = recordRepository.findByBatchIdOrderBySequenceNumber(batchId);
        if (records.isEmpty()) {
            throw new RipsValidationException("No hay registros en el batch: " + batchId);
        }

        String codigoPrestador = extractCodigoPrestador(batch, records);

        List<com.kinplatform.kin.medical.billing.rips.ubl.UblExtension> extensions = records.stream()
                .map(record -> UblExtensionFactory.fromRecord(record, codigoPrestador).getExtensions().get(0))
                .toList();

        RipsInteroperabilidadPayload payload = RipsInteroperabilidadPayload.builder()
                .extensions(extensions)
                .build();

        try {
            return xmlMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializando UBL a XML para batch {}: {}", batchId, e.getMessage(), e);
            throw new RipsValidationException("Error generando XML UBL: " + e.getMessage(), e);
        }
    }

    private String extractCodigoPrestador(RipsBatch batch, List<RipsRecord> records) {
        if (!records.isEmpty()) {
            Map<String, Object> data = parseRipsLineData(records.get(0).getRipsLineData());
            Object codPrestador = data.get("codigoPrestador");
            if (codPrestador != null && !codPrestador.toString().trim().isEmpty()) {
                return codPrestador.toString().trim();
            }
        }
        return batch.getOrganizationId().toString().substring(0, Math.min(10, batch.getOrganizationId().toString().length()));
    }

    private Map<String, Object> parseRipsLineData(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}