package com.kinplatform.kin.health.verification.domain;

import java.util.List;

/**
 * Resultado consolidado de la verificación clínica de un turno de análisis.
 *
 * <p>Se inyecta en el prompt del modelo: cuando el estado es distinto de
 * {@code VERIFIED} la IA queda instruida para NO inventar códigos CIE ni
 * cifras oficiales y para declarar explícitamente que no se pudo verificar.</p>
 */
public record ClinicalVerification(
        VerificationStatus status,
        List<DiagnosticMatch> diagnosticMatches,
        List<GlobalStatistic> globalStatistics,
        String message) {

    public ClinicalVerification {
        diagnosticMatches = diagnosticMatches == null ? List.of() : diagnosticMatches;
        globalStatistics = globalStatistics == null ? List.of() : globalStatistics;
        message = message == null ? "" : message;
    }

    public boolean verified() {
        return status == VerificationStatus.VERIFIED;
    }
}
