package com.kinplatform.kin.health.dashboard.domain;

import java.util.List;

/**
 * Plan de cuidado personalizado del paciente (ADR-030).
 *
 * <p>Generado de forma determinista por {@link CareRecommendationRegistry}: a
 * partir de las condiciones identificadas (perfil + historial), selecciona
 * recomendaciones de plantilla combinadas y deduplicadas. Sin LLM.</p>
 */
public record CarePlan(List<String> recommendations, List<String> sourceConditions, List<CareRecommendation> detailed) {

    public CarePlan {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        sourceConditions = sourceConditions == null ? List.of() : List.copyOf(sourceConditions);
        detailed = detailed == null ? List.of() : List.copyOf(detailed);
    }

    public static CarePlan empty() {
        return new CarePlan(List.of(), List.of(), List.of());
    }

    /**
     * Recomendación detallada del plan.
     *
     * @param condition  condición que la originó
     * @param advice     consejo concreto
     * @param priority   nivel (ALTA/MEDIA/BAJA)
     */
    public record CareRecommendation(String condition, String advice, String priority) {}
}
