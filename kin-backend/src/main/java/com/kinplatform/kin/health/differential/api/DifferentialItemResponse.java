package com.kinplatform.kin.health.differential.api;

import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.util.List;
import java.util.UUID;

/**
 * Item del diagnóstico diferencial en el endpoint REST (ADR-029).
 */
public record DifferentialItemResponse(
        UUID conditionId,
        String condition,
        String description,
        double probability,
        Severity severity,
        Urgency urgency,
        List<String> matchedSymptoms,
        List<RiskFactorResponse> riskFactors,
        List<RecommendedTestResponse> recommendedTests,
        String reasoning) {

    public static DifferentialItemResponse from(com.kinplatform.kin.health.differential.domain.DifferentialItem item) {
        return new DifferentialItemResponse(
                item.conditionId(),
                item.name(),
                item.description(),
                item.probability(),
                item.severity(),
                item.urgency(),
                item.matchedSymptoms(),
                item.riskFactors().stream().map(RiskFactorResponse::from).toList(),
                item.recommendedTests().stream()
                        .map(RecommendedTestResponse::from)
                        .toList(),
                item.reasoning());
    }

    public record RiskFactorResponse(String factor, double weight, String description) {
        static RiskFactorResponse from(RiskFactor riskFactor) {
            return new RiskFactorResponse(riskFactor.factor(), riskFactor.weight(), riskFactor.description());
        }
    }

    public record RecommendedTestResponse(String test, String description) {
        static RecommendedTestResponse from(RecommendedTest test) {
            return new RecommendedTestResponse(test.test(), test.description());
        }
    }
}
