package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.engine.DeterministicId;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;

import java.util.List;
import java.util.UUID;

/**
 * Oportunidad identificada y clasificada por un OpportunityAnalyzer.
 * Value Object inmutable.
 *
 * <p>Contiene la categoría, prioridad (1-10), impacto esperado, esfuerzo
 * estimado, confianza, explicación (con evidencia), reglas aplicadas y la
 * versión del engine que la produjo.</p>
 */
public record Opportunity(
    UUID id,
    OpportunityCategory category,
    String title,
    String description,
    int priority,
    ImpactLevel impactLevel,
    EffortLevel effortLevel,
    double confidence,
    OpportunityExplanation explanation,
    List<String> appliedRules,
    AnalyzedDimension relatedDimension,
    String engineVersion
) {

    public Opportunity {
        priority = Math.max(1, Math.min(10, priority));
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        explanation = explanation == null ? OpportunityExplanation.of(List.of(), "", "", "") : explanation;
        appliedRules = appliedRules == null ? List.of() : List.copyOf(appliedRules);
        engineVersion = engineVersion == null ? "" : engineVersion;
    }

    public static Opportunity create(OpportunityCategory category, String title, String description,
                                     int priority, ImpactLevel impactLevel, EffortLevel effortLevel,
                                     double confidence, OpportunityExplanation explanation,
                                     List<String> appliedRules, AnalyzedDimension relatedDimension,
                                     String engineVersion) {
        var id = DeterministicId.from(category.name(), title, description);
        return new Opportunity(id, category, title, description, priority, impactLevel, effortLevel,
            confidence, explanation, appliedRules, relatedDimension, engineVersion);
    }
}
