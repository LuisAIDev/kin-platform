package com.kinplatform.kin.health.dashboard.api;

import com.kinplatform.kin.health.dashboard.domain.CarePlan;
import java.util.List;

/**
 * Plan de cuidado del paciente (ADR-030) para el endpoint REST.
 */
public record CarePlanResponse(
        List<String> recommendations, List<String> sourceConditions, List<CareRecommendationResponse> detailed) {

    public static CarePlanResponse from(CarePlan plan) {
        return new CarePlanResponse(
                plan.recommendations(),
                plan.sourceConditions(),
                plan.detailed().stream().map(CareRecommendationResponse::from).toList());
    }

    public record CareRecommendationResponse(String condition, String advice, String priority) {
        static CareRecommendationResponse from(CarePlan.CareRecommendation rec) {
            return new CareRecommendationResponse(rec.condition(), rec.advice(), rec.priority());
        }
    }
}
