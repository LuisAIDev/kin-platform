package com.kinplatform.kin.health.verification.domain;

/**
 * Estado de la verificación clínica contra la OMS en un turno de análisis.
 */
public enum VerificationStatus {
    /** Códigos CIE verificados contra la ICD-API (resultados disponibles). */
    VERIFIED,
    /** La consulta se hizo pero no arrojó coincidencias: dato NO verificado. */
    UNVERIFIED,
    /** Sin credenciales/configuración de la ICD-API: no se pudo verificar. */
    UNCONFIGURED,
    /** La ICD-API no está disponible (Circuit Breaker abierto o error): no se pudo verificar. */
    UNAVAILABLE
}
