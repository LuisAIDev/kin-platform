package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.kin.enterprise.valueobjects.KpiSet;
import com.kinplatform.kin.enterprise.valueobjects.LeanCanvas;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.enterprise.valueobjects.RiskMatrix;
import com.kinplatform.kin.enterprise.valueobjects.Roadmap;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskResult;

/**
 * Entrada tipada del {@code EnterpriseScoreEngine} (Fase 10, Milestone 2D).
 *
 * <p>Porta los value objects del proyecto empresarial que el motor de
 * puntuación consume para calcular el {@link EnterpriseScore} multidimensional:
 * las ocho dimensiones se derivan de forma determinista a partir de los planes
 * ya producidos y de los resultados del pipeline.</p>
 */
public record EnterpriseScoreInput(
    ProjectContext context,
    LeanCanvas canvas,
    MarketPlan marketPlan,
    InnovationPlan innovationPlan,
    FinancialPlan financialPlan,
    RiskMatrix riskMatrix,
    Roadmap roadmap,
    KpiSet kpis,
    RecommendationResult recommendations,
    OpportunityResult opportunities,
    KnowledgeResult knowledge,
    RiskResult riskResult
) implements EngineInput {
}
