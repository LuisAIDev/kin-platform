package com.kinplatform.kin.context;

public record EvaluationPolicies(
    double minimumCoverage,
    double minimumConfidence,
    int minimumConversationDepth,
    int minimumCriticalDimensions,
    double reportCoverageThreshold,
    double reportConfidenceThreshold
) {

    public static EvaluationPolicies defaults() {
        return new EvaluationPolicies(0.65, 0.5, 5, 3, 0.65, 0.6);
    }
}
