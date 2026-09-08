package com.kinplatform.kin.health.verification.domain;

/**
 * Coincidencia de un código de diagnóstico devuelta por la ICD-API de la OMS.
 */
public record DiagnosticMatch(
        String code,
        String title,
        String searchTerm,
        String release) {

    public DiagnosticMatch {
        code = code == null ? "" : code;
        title = title == null ? "" : title;
        searchTerm = searchTerm == null ? "" : searchTerm;
        release = release == null ? "" : release;
    }
}
