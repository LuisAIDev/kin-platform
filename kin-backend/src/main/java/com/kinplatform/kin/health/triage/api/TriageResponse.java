package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.domain.TriageResult;
import java.util.List;

/**
 * Respuesta del endpoint de triaje (ADR-028).
 *
 * <p>Incluye el aviso obligatorio: el triaje es una herramienta de apoyo a la
 * decisión y no sustituye el diagnóstico médico profesional.</p>
 */
public record TriageResponse(
        String status, List<TriageConditionResponse> results, List<String> unrecognizedSymptoms, String disclaimer) {

    public static final String DISCLAIMER = "Esta herramienta es de apoyo informativo y no sustituye la evaluación "
            + "ni el diagnóstico de un profesional de la salud. Ante síntomas graves "
            + "o persistentes, consulta a un médico.";

    public static TriageResponse success(TriageResult result) {
        return new TriageResponse(
                "SUCCESS",
                result.results().stream().map(TriageConditionResponse::from).toList(),
                result.unrecognizedSymptoms(),
                DISCLAIMER);
    }

    public static TriageResponse empty(List<String> unrecognizedSymptoms) {
        return new TriageResponse("NO_MATCH", List.of(), unrecognizedSymptoms, DISCLAIMER);
    }
}
