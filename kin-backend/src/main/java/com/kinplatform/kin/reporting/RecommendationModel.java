package com.kinplatform.kin.reporting;

/**
 * Configuración de umbrales del RecommendationEngine. Value Object inmutable.
 */
public class RecommendationModel {

    private final int lowScoreThreshold;
    private final int highScoreThreshold;
    private final double minCoverageForMature;
    private final String version;
    private final String description;

    public RecommendationModel(int lowScoreThreshold, int highScoreThreshold,
                               double minCoverageForMature, String version, String description) {
        this.lowScoreThreshold = lowScoreThreshold;
        this.highScoreThreshold = highScoreThreshold;
        this.minCoverageForMature = minCoverageForMature;
        this.version = version;
        this.description = description;
    }

    public int lowScoreThreshold() {
        return lowScoreThreshold;
    }

    public int highScoreThreshold() {
        return highScoreThreshold;
    }

    public double minCoverageForMature() {
        return minCoverageForMature;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    public static RecommendationModel defaultModel() {
        return new RecommendationModel(40, 70, 0.6, "v1",
            "Modelo de recomendaciones basado en brechas de dimensiones y score de viabilidad");
    }
}
