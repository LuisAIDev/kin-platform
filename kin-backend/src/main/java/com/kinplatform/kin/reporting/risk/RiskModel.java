package com.kinplatform.kin.reporting.risk;

/**
 * Configuración de umbrales del RiskEngine. Value Object inmutable.
 */
public class RiskModel {

    private final int highSeverityCoverageThreshold;
    private final String version;
    private final String description;

    public RiskModel(int highSeverityCoverageThreshold, String version, String description) {
        this.highSeverityCoverageThreshold = highSeverityCoverageThreshold;
        this.version = version;
        this.description = description;
    }

    public int highSeverityCoverageThreshold() {
        return highSeverityCoverageThreshold;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    public static RiskModel defaultModel() {
        return new RiskModel(40,
            "v1", "Modelo de riesgos basado en brechas de dimensiones del proyecto");
    }
}
