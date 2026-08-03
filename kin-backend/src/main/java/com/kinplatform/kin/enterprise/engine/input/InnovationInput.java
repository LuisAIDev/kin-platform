package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;

/**
 * Entrada tipada del {@code InnovationEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor de innovación consume para construir el
 * {@code InnovationPlan}: el contexto del proyecto y las oportunidades ya
 * identificadas por el pipeline. El motor reutiliza {@link OpportunityResult}
 * sin recalcular el análisis de oportunidades.</p>
 */
public record InnovationInput(
    ProjectContext context,
    OpportunityResult opportunities,
    KnowledgeResult knowledge
) implements EngineInput {
}
