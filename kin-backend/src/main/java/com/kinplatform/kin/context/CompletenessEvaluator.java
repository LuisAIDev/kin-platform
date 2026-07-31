package com.kinplatform.kin.context;

import java.util.List;

public class CompletenessEvaluator {

    private static final List<AnalyzedDimension> CRITICAL_DIMENSIONS = List.of(
        AnalyzedDimension.PROBLEM,
        AnalyzedDimension.SOLUTION,
        AnalyzedDimension.TARGET_CUSTOMER,
        AnalyzedDimension.REVENUE_MODEL
    );

    private final EvaluationPolicies policies;

    public CompletenessEvaluator(EvaluationPolicies policies) {
        this.policies = policies;
    }

    public CompletenessEvaluation evaluate(ProjectContext context) {
        int total = AnalyzedDimension.values().length;
        int covered = context.knownDimensionsCount();
        double coverage = total > 0 ? (double) covered / total : 0.0;

        var missing = context.missingDimensions();
        var criticalMissing = missing.stream()
            .filter(CRITICAL_DIMENSIONS::contains)
            .toList();

        double confidence = computeConfidence(context, coverage);
        double quality = computeQualityOfInformation(context, coverage);
        var maturity = computeMaturity(coverage);
        var viability = computeViability(confidence, coverage);
        var recommendation = computeRecommendation(coverage, confidence, context, criticalMissing);
        boolean ready = computeReadyForReport(coverage, confidence, context, criticalMissing);

        return new CompletenessEvaluation(
            coverage, missing, criticalMissing, confidence,
            maturity, viability, quality,
            List.of(), List.of(), List.of(),
            recommendation, ready, covered, total
        );
    }

    private double computeConfidence(ProjectContext context, double coverage) {
        double exchangeFactor = Math.min(1.0, context.exchangeCount() / 10.0);
        return (coverage * 0.6) + (exchangeFactor * 0.4);
    }

    private double computeQualityOfInformation(ProjectContext context, double coverage) {
        double base = 0.3;
        double coverageBonus = Math.min(0.5, context.knownDimensionsCount() * 0.1);
        double depthBonus = context.exchangeCount() >= 3 ? 0.2 : 0.0;
        return Math.min(1.0, base + coverageBonus + depthBonus);
    }

    private CompletenessEvaluation.MaturityLevel computeMaturity(double coverage) {
        if (coverage < 0.3) return CompletenessEvaluation.MaturityLevel.EARLY;
        if (coverage < 0.7) return CompletenessEvaluation.MaturityLevel.DEVELOPING;
        return CompletenessEvaluation.MaturityLevel.MATURE;
    }

    private CompletenessEvaluation.ViabilityLevel computeViability(double confidence, double coverage) {
        double score = (confidence * 0.5) + (coverage * 0.5);
        if (score < 0.3) return CompletenessEvaluation.ViabilityLevel.LOW;
        if (score < 0.6) return CompletenessEvaluation.ViabilityLevel.MEDIUM;
        if (score < 0.8) return CompletenessEvaluation.ViabilityLevel.HIGH;
        return CompletenessEvaluation.ViabilityLevel.VERY_HIGH;
    }

    private CompletenessEvaluation.RecommendationLevel computeRecommendation(
            double coverage, double confidence, ProjectContext context,
            List<AnalyzedDimension> criticalMissing) {
        if (context.reportGenerated()) return CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT;
        if (coverage >= policies.minimumCoverage()
            && confidence >= policies.minimumConfidence()
            && context.exchangeCount() >= policies.minimumConversationDepth()
            && criticalMissing.size() <= (AnalyzedDimension.values().length - policies.minimumCriticalDimensions())) {
            return CompletenessEvaluation.RecommendationLevel.READY_FOR_REPORT;
        }
        if (coverage >= 0.4) return CompletenessEvaluation.RecommendationLevel.READY_FOR_REVIEW;
        return CompletenessEvaluation.RecommendationLevel.EXPLORE_MORE;
    }

    private boolean computeReadyForReport(
            double coverage, double confidence, ProjectContext context,
            List<AnalyzedDimension> criticalMissing) {
        if (context.reportGenerated()) return false;
        return coverage >= policies.minimumCoverage()
            && confidence >= policies.minimumConfidence()
            && context.exchangeCount() >= policies.minimumConversationDepth()
            && criticalMissing.size() <= (AnalyzedDimension.values().length - policies.minimumCriticalDimensions());
    }
}
