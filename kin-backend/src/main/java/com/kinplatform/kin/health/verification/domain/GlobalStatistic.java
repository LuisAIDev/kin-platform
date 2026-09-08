package com.kinplatform.kin.health.verification.domain;

/**
 * Estadística global de salud devuelta por el Global Health Observatory (GHO).
 */
public record GlobalStatistic(
        String indicatorCode,
        String indicatorLabel,
        String countryCode,
        String value,
        String year) {

    public GlobalStatistic {
        indicatorCode = indicatorCode == null ? "" : indicatorCode;
        indicatorLabel = indicatorLabel == null ? "" : indicatorLabel;
        countryCode = countryCode == null ? "" : countryCode;
        value = value == null ? "" : value;
        year = year == null ? "" : year;
    }
}
