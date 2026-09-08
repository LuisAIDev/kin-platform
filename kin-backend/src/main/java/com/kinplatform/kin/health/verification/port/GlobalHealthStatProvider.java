package com.kinplatform.kin.health.verification.port;

import com.kinplatform.kin.health.verification.domain.GlobalStatistic;
import java.util.List;

/**
 * Puente con el Global Health Observatory (GHO) de la OMS: estadísticas
 * globales de salud por indicador y país.
 */
public interface GlobalHealthStatProvider {

    /**
     * Consulta un indicador del GHO.
     *
     * @param indicatorCode código oficial del indicador en el catálogo GHO.
     * @param countryCode código de país ISO 3166-1 alpha-3 (vacío = GLOBAL).
     * @return estadísticas; nunca {@code null}.
     * @throws WhoHealthDataSourceException si la llamada falla.
     */
    List<GlobalStatistic> indicator(String indicatorCode, String countryCode);
}
