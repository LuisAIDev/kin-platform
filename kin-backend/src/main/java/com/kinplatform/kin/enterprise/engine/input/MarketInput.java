package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;

/**
 * Entrada tipada del {@code MarketEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los datos que el motor de mercado consume para construir el
 * {@link MarketPlan}: el contexto del proyecto y los resultados del pipeline.
 * Los hechos verificados de {@link KnowledgeResult} se priorizan cuando
 * existen; en modo offline (resultado vacío) el motor opera sin datos
 * externos.</p>
 */
public record MarketInput(
    ProjectContext context,
    RecommendationResult recommendations,
    OpportunityResult opportunities,
    KnowledgeResult knowledge
) implements EngineInput {
}
