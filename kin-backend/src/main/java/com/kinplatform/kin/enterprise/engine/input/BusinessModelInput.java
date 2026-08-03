package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;

/**
 * Entrada tipada del {@code BusinessModelEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor de modelo de negocio consume para construir
 * el Lean Canvas: el contexto del proyecto y los resultados deterministas del
 * pipeline (recomendaciones, oportunidades y conocimiento externo). El
 * {@link KnowledgeResult} puede estar vacío (modo offline).</p>
 */
public record BusinessModelInput(
    ProjectContext context,
    RecommendationResult recommendations,
    OpportunityResult opportunities,
    KnowledgeResult knowledge
) implements EngineInput {
}
