package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Plan de innovación del proyecto empresarial (value object).
 *
 * <p>Representa la estrategia de innovación: nivel de novedad
 * ({@link InnovationLevel}), factores diferenciales, barreras de imitación,
 * hoja de ruta de I+D y recomendaciones de investigación. Producido por
 * {@code InnovationEngine}.</p>
 */
public record InnovationPlan(
    InnovationLevel innovationLevel,
    List<String> differentiators,
    String defensibility,
    List<String> innovationRoadmap,
    List<String> researchRecommendations
) {

    public InnovationPlan {
        if (innovationLevel == null) {
            throw new IllegalArgumentException("'innovationLevel' no puede ser null.");
        }
        differentiators = ValueObjects.immutableNotBlank(differentiators, "differentiators");
        ValueObjects.requireNotBlank(defensibility, "defensibility");
        innovationRoadmap = ValueObjects.immutableNotBlank(innovationRoadmap, "innovationRoadmap");
        researchRecommendations = ValueObjects.immutableNotBlank(researchRecommendations, "researchRecommendations");
    }

    /**
     * Crea un plan de innovación vacío (nivel incremental y listas vacías).
     */
    public static InnovationPlan empty() {
        return new InnovationPlan(InnovationLevel.INCREMENTAL,
            List.of(), "Sin definir", List.of(), List.of());
    }

    /**
     * Crea un plan de innovación completo.
     */
    public static InnovationPlan of(InnovationLevel innovationLevel,
                                    List<String> differentiators, String defensibility,
                                    List<String> innovationRoadmap,
                                    List<String> researchRecommendations) {
        return new InnovationPlan(innovationLevel, differentiators, defensibility,
            innovationRoadmap, researchRecommendations);
    }
}
