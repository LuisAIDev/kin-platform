package com.kinplatform.billing.glosa;

import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Matcheo automatico glosa -> linea RIPS.
 *
 * 1. Si la glosa trae ripsRecordId, se resuelve directo.
 * 2. Si trae ripsBatchId + glosaCode, se busca en las lineas del batch la que
 *    contenga ese codigo (consulta/procedimiento/servicio).
 */
@Component
@RequiredArgsConstructor
public class MatchingEngine {

    private final RipsRecordRepository recordRepository;

    public Optional<RipsRecord> match(Glosa glosa) {
        if (glosa.getRipsRecordId() != null) {
            Optional<RipsRecord> byId = recordRepository.findById(glosa.getRipsRecordId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (glosa.getRipsBatchId() != null && glosa.getGlosaCode() != null) {
            return recordRepository.findByBatchIdOrderBySequenceNumber(glosa.getRipsBatchId()).stream()
                    .filter(record -> containsCode(record.getRipsLineData(), glosa.getGlosaCode()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private boolean containsCode(String ripsLineData, String code) {
        return ripsLineData != null && code != null && !code.isBlank() && ripsLineData.contains(code);
    }
}
