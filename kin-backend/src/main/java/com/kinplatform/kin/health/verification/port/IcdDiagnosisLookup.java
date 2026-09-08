package com.kinplatform.kin.health.verification.port;

import com.kinplatform.kin.health.verification.domain.DiagnosticMatch;
import java.util.List;

/**
 * Puente con la ICD-API de la OMS: búsqueda de códigos de diagnóstico.
 */
public interface IcdDiagnosisLookup {

    /**
     * Busca códigos CIE (ICD-11 MMS por defecto) para un término.
     *
     * @param term término libre (síntoma, hallazgo o valor anómalo).
     * @return coincidencias (posiblemente vacío); nunca {@code null}.
     * @throws WhoHealthDataSourceException si la llamada falla o no está disponible.
     */
    List<DiagnosticMatch> search(String term);
}
