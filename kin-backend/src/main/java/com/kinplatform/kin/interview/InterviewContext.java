package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;

import java.util.Set;
import java.util.UUID;

/**
 * Proyección inmutable del contexto del proyecto que la entrevista necesita
 * (ADR-015).
 *
 * <p>Contiene únicamente los datos relevantes para la recopilación: identidad
 * del proyecto, título, categoría y las dimensiones ya cubiertas (para no volver
 * a preguntar sobre ellas). {@code coveredDimensions} se copia de forma
 * defensiva y nunca se modifica desde fuera.</p>
 */
public record InterviewContext(
    UUID projectId,
    String projectTitle,
    String projectCategory,
    Set<AnalyzedDimension> coveredDimensions
) {

    public InterviewContext {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser null");
        }
        projectTitle = projectTitle == null ? "" : projectTitle;
        projectCategory = projectCategory == null ? "" : projectCategory;
        coveredDimensions = coveredDimensions == null ? Set.of() : Set.copyOf(coveredDimensions);
    }

    public static InterviewContext of(UUID projectId, String projectTitle, String projectCategory,
                                      Set<AnalyzedDimension> coveredDimensions) {
        return new InterviewContext(projectId, projectTitle, projectCategory, coveredDimensions);
    }

    /**
     * Contexto mínimo de un proyecto recién creado (sin datos conocidos).
     */
    public static InterviewContext ofProject(UUID projectId) {
        return new InterviewContext(projectId, "", "", Set.of());
    }

    public double coverageRatio() {
        return (double) coveredDimensions.size() / AnalyzedDimension.values().length;
    }

    public boolean covers(AnalyzedDimension dimension) {
        return coveredDimensions.contains(dimension);
    }
}
