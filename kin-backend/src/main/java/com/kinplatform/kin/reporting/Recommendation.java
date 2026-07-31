package com.kinplatform.kin.reporting;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.engine.DeterministicId;

import java.util.List;
import java.util.UUID;

/**
 * Recomendación estructurada generada por el RecommendationEngine.
 * Es un Value Object inmutable y auditable.
 */
public record Recommendation(
    UUID id,
    RecommendationCategory category,
    String title,
    String description,
    int priority,
    ImpactLevel impactLevel,
    EffortLevel effortLevel,
    AnalyzedDimension relatedDimension,
    List<String> actionableSteps,
    String expectedOutcome,
    RecommendationExplanation explanation
) {

    public Recommendation {
        actionableSteps = actionableSteps == null ? List.of() : List.copyOf(actionableSteps);
        explanation = explanation == null
            ? RecommendationExplanation.of(List.of(), "", "")
            : explanation;
        if (priority < 1) priority = 1;
        if (priority > 10) priority = 10;
    }

    public static Recommendation create(RecommendationCategory category, String title, String description,
                                        int priority, ImpactLevel impactLevel, EffortLevel effortLevel,
                                        AnalyzedDimension relatedDimension, List<String> actionableSteps,
                                        String expectedOutcome, RecommendationExplanation explanation) {
        var id = DeterministicId.from(category.name(), title, description);
        return new Recommendation(id, category, title, description, priority, impactLevel,
            effortLevel, relatedDimension, actionableSteps, expectedOutcome, explanation);
    }
}
