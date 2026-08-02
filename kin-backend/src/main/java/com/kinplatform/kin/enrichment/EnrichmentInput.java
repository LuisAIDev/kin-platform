package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.knowledge.KnowledgeResult;

import java.util.EnumSet;
import java.util.Set;

/**
 * Entrada canonizada del motor de enriquecimiento (ADR-016, ADR-005/009).
 * Inmutable.
 *
 * <p>Envuelve el contexto del proyecto y el {@link KnowledgeResult} producido
 * por la Fase 6, junto con las categorías objetivo y el umbral mínimo de
 * relevancia. Implementa {@link EngineInput} para integrarse con la
 * infraestructura común de motores.</p>
 *
 * <p>Decisiones en Java: qué categorías se consideran y con qué umbral mínimo
 * se acepta una evidencia.</p>
 */
public record EnrichmentInput(
    ProjectContext context,
    KnowledgeResult knowledge,
    Set<EvidenceCategory> categories,
    double minScore
) implements EngineInput {

    public EnrichmentInput {
        categories = (categories == null || categories.isEmpty())
            ? EnumSet.allOf(EvidenceCategory.class)
            : Set.copyOf(categories);
        minScore = Math.max(0.0, Math.min(1.0, minScore));
    }

    public static EnrichmentInput of(ProjectContext context, KnowledgeResult knowledge) {
        return new EnrichmentInput(context, knowledge, EnumSet.allOf(EvidenceCategory.class), 0.0);
    }

    public static EnrichmentInput of(ProjectContext context, KnowledgeResult knowledge,
                                     Set<EvidenceCategory> categories, double minScore) {
        return new EnrichmentInput(context, knowledge, categories, minScore);
    }
}
