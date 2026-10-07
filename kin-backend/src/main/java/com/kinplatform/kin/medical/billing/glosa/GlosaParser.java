package com.kinplatform.kin.medical.billing.glosa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parser de archivos planos de glosas de EPS.
 *
 * Formato (columnas, mismo orden; delimitador segun EPS):
 * EPS_GLOSA_NUMBER | RIPS_BATCH_ID | RIPS_RECORD_ID | TYPE | CODE | DESCRIPTION | ORIGINAL_VALUE | GLOSA_VALUE
 *
 * Colsanitas usa '|' y Sura usa ';'. Las lineas vacias y las que inician con
 * '#' se ignoran. Las columnas de UUID en blanco quedan en null.
 */
@Component
@Slf4j
public class GlosaParser {

    public List<Glosa> parse(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String delimiter = content.contains("|") ? "\\|" : ";";
        return parse(content, delimiter);
    }

    public List<Glosa> parse(String content, String delimiterRegex) {
        List<Glosa> result = new ArrayList<>();
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split(delimiterRegex, -1);
            if (fields.length < 8) {
                log.warn("Linea de glosa ignorada ({} columnas): {}", fields.length, line);
                continue;
            }
            try {
                result.add(Glosa.builder()
                        .epsGlosaNumber(trimToNull(fields[0]))
                        .ripsBatchId(parseUuid(fields[1]))
                        .ripsRecordId(parseUuid(fields[2]))
                        .glosaType(Glosa.GlosaType.valueOf(fields[3].trim().toUpperCase()))
                        .glosaCode(trimToNull(fields[4]))
                        .glosaDescription(trimToNull(fields[5]))
                        .originalValueCop(parseDecimal(fields[6]))
                        .glosaValueCop(parseDecimal(fields[7]))
                        .status(Glosa.GlosaStatus.RECEIVED)
                        .build());
            } catch (IllegalArgumentException e) {
                log.warn("Linea de glosa ignorada (dato invalido): {}", line);
            }
        }
        return result;
    }

    private String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID parseUuid(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : UUID.fromString(trimmed);
    }

    private BigDecimal parseDecimal(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? BigDecimal.ZERO : new BigDecimal(trimmed);
    }
}

