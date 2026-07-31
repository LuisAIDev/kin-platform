package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationCategory;

import java.util.List;

/**
 * Sección de recomendaciones del reporte: reutiliza el VO {@link Recommendation}
 * ya producido por el RecommendationEngine (lista ya ordenada).
 */
public record RecommendationsSection(
    List<Recommendation> recommendations,
    int priority,
    double confidence,
    RecommendationCategory dominantCategory
) implements ReportSection {

    public RecommendationsSection {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        priority = Math.max(0, Math.min(10, priority));
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        dominantCategory = dominantCategory == null ? RecommendationCategory.VALIDATION : dominantCategory;
    }

    @Override
    public String sectionName() {
        return "Recommendations";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.ANALYTIC;
    }

    public boolean isEmpty() {
        return recommendations.isEmpty();
    }

    public static RecommendationsSection empty() {
        return new RecommendationsSection(List.of(), 0, 0.0, RecommendationCategory.VALIDATION);
    }
}
