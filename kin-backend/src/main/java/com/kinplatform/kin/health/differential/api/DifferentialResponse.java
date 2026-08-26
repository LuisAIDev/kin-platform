package com.kinplatform.kin.health.differential.api;

import com.kinplatform.kin.health.differential.domain.DifferentialResult;
import java.util.List;

/**
 * Respuesta del endpoint de diagnóstico diferencial (ADR-029).
 *
 * <p>Incluye el aviso obligatorio: el diagnóstico diferencial es una
 * herramienta de apoyo informativo y no sustituye la evaluación médica
 * profesional.</p>
 */
public record DifferentialResponse(
        String status, List<DifferentialItemResponse> items, List<String> unrecognizedSymptoms, String disclaimer) {

    public static final String DISCLAIMER = "El diagnóstico diferencial es una herramienta de apoyo informativo y "
            + "no sustituye la evaluación ni el diagnóstico de un profesional de la salud. "
            + "Las pruebas sugeridas requieren indicación médica.";

    public static DifferentialResponse success(DifferentialResult result) {
        return new DifferentialResponse(
                "SUCCESS",
                result.items().stream().map(DifferentialItemResponse::from).toList(),
                result.unrecognizedSymptoms(),
                DISCLAIMER);
    }

    public static DifferentialResponse empty(List<String> unrecognizedSymptoms) {
        return new DifferentialResponse("NO_MATCH", List.of(), unrecognizedSymptoms, DISCLAIMER);
    }
}
