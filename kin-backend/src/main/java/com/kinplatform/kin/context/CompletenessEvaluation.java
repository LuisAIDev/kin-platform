package com.kinplatform.kin.context;

import java.util.List;

public record CompletenessEvaluation(
    double coveragePercent,
    List<AnalyzedDimension> missingDimensions,
    List<AnalyzedDimension> criticalMissingDimensions,
    double confidenceScore,
    MaturityLevel maturityLevel,
    ViabilityLevel viabilityLevel,
    double qualityOfInformation,
    List<String> detectedRisks,
    List<String> detectedOpportunities,
    List<String> missingCriticalInformation,
    RecommendationLevel recommendationLevel,
    boolean readyForReport,
    int dimensionsCovered,
    int totalDimensions
) {

    public enum MaturityLevel {
        EARLY,
        DEVELOPING,
        MATURE
    }

    public enum ViabilityLevel {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }

    public enum RecommendationLevel {
        EXPLORE_MORE,
        READY_FOR_REVIEW,
        READY_FOR_REPORT
    }

    public boolean isComplete() {
        return missingDimensions.isEmpty() || readyForReport;
    }

    public static CompletenessEvaluation empty() {
        return new CompletenessEvaluation(
            0.0, List.of(), List.of(), 0.0,
            MaturityLevel.EARLY, ViabilityLevel.LOW, 0.0,
            List.of(), List.of(), List.of(),
            RecommendationLevel.EXPLORE_MORE, false,
            0, AnalyzedDimension.values().length
        );
    }
}
