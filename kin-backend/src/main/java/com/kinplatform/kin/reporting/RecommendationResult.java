package com.kinplatform.kin.reporting;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;

/**
 * Resultado inmutable del RecommendationEngine. Contiene las recomendaciones
 * generadas junto con metadatos de prioridad, confianza, categoría dominante
 * y trazabilidad del engine que las produjo.
 *
 * <p>Implementa {@link EngineResult} para compartir el contrato de resultados
 * de la infraestructura común de motores sin perder tipado fuerte.</p>
 */
public record RecommendationResult(
    List<Recommendation> recommendations,
    int priority,
    double confidence,
    RecommendationCategory category,
    String explanation,
    String generatedBy,
    String engineVersion
) implements EngineResult {

    public RecommendationResult {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        if (priority < 0) priority = 0;
        if (priority > 10) priority = 10;
        if (confidence < 0.0) confidence = 0.0;
        if (confidence > 1.0) confidence = 1.0;
    }

    public boolean hasRecommendations() {
        return !recommendations.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return recommendations.isEmpty();
    }

    public static RecommendationResult empty() {
        return new RecommendationResult(
            List.of(), 0, 0.0, RecommendationCategory.VALIDATION,
            "No se generaron recomendaciones.", "", ""
        );
    }
}
