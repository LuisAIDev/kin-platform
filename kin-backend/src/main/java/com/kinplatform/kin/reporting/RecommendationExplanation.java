package com.kinplatform.kin.reporting;

import java.util.List;

/**
 * Explicación auditable de una recomendación: qué información se utilizó,
 * qué regla se aplicó y por qué fue generada.
 */
public record RecommendationExplanation(
    List<String> usedInformation,
    String appliedRule,
    String reason
) {

    public RecommendationExplanation {
        usedInformation = usedInformation == null ? List.of() : List.copyOf(usedInformation);
    }

    public static RecommendationExplanation of(List<String> usedInformation, String appliedRule, String reason) {
        return new RecommendationExplanation(usedInformation, appliedRule, reason);
    }
}
