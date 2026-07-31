package com.kinplatform.kin.reporting.opportunity;

/**
 * Configuración de umbrales del OpportunityEngine. Value Object inmutable.
 */
public class OpportunityModel {

    private final int highPriorityThreshold;
    private final int mediumPriorityThreshold;
    private final String version;
    private final String description;

    public OpportunityModel(int highPriorityThreshold, int mediumPriorityThreshold,
                            String version, String description) {
        this.highPriorityThreshold = highPriorityThreshold;
        this.mediumPriorityThreshold = mediumPriorityThreshold;
        this.version = version;
        this.description = description;
    }

    public int highPriorityThreshold() {
        return highPriorityThreshold;
    }

    public int mediumPriorityThreshold() {
        return mediumPriorityThreshold;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    public static OpportunityModel defaultModel() {
        return new OpportunityModel(8, 5, "v1",
            "Modelo de oportunidades basado en brechas de dimensiones, score y señales detectadas");
    }
}
