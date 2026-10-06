package com.kinplatform.platform.enterprise.engine.input;

import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.engine.EngineInput;
import com.kinplatform.platform.enterprise.valueobjects.EnterpriseScore;
import com.kinplatform.platform.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.platform.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.platform.enterprise.valueobjects.KpiSet;
import com.kinplatform.platform.enterprise.valueobjects.LeanCanvas;
import com.kinplatform.platform.enterprise.valueobjects.MarketPlan;
import com.kinplatform.platform.enterprise.valueobjects.RiskMatrix;
import com.kinplatform.platform.enterprise.valueobjects.Roadmap;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.platform.reporting.RecommendationResult;
import com.kinplatform.platform.reporting.opportunity.OpportunityResult;
import com.kinplatform.platform.reporting.risk.RiskResult;

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






