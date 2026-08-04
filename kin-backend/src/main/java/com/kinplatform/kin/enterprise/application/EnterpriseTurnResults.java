package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskResult;

import java.util.UUID;

/**
 * Resultados reales del pipeline capturados al completar un turno
 * {@code REPORT} (Fase 10, Milestone 3C).
 *
 * <p>Transporta los cuatro resultados deterministas que el pipeline produce en
 * cada turno y que consumen los motores del proyecto empresarial:
 * recomendaciones ({@code RecommendationResult}), oportunidades
 * ({@code OpportunityResult}), conocimiento externo verificado
 * ({@code KnowledgeResult}) y riesgo ({@code RiskResult}). Se capturan en el
 * runtime ({@code KinMethod}) y se entregan a la generación del proyecto
 * empresarial vía {@link EnterprisePipelineResultStore}; los resultados se
 * reutilizan exactamente como los produjo el pipeline, sin recalcular ni volver
 * a ejecutar motores.</p>
 *
 * <p>Invariante: los resultados opcionales adoptan su patrón {@code empty()}
 * cuando no se aportan (modo offline-first, mismo comportamiento que
 * {@code EnterpriseGenerationRequest}).</p>
 *
 * @param projectId     identificador del proyecto de KIN origen
 * @param recommendations recomendaciones del pipeline (o {@code null} → vacío)
 * @param opportunities  oportunidades del pipeline (o {@code null} → vacío)
 * @param knowledge      conocimiento externo verificado (o {@code null} → vacío)
 * @param riskResult     riesgo del pipeline (o {@code null} → vacío)
 */
public record EnterpriseTurnResults(
    UUID projectId,
    RecommendationResult recommendations,
    OpportunityResult opportunities,
    KnowledgeResult knowledge,
    RiskResult riskResult
) {

    public EnterpriseTurnResults {
        recommendations = recommendations == null ? RecommendationResult.empty() : recommendations;
        opportunities = opportunities == null ? OpportunityResult.empty() : opportunities;
        knowledge = knowledge == null ? KnowledgeResult.empty() : knowledge;
        riskResult = riskResult == null ? RiskResult.empty() : riskResult;
    }

    /**
     * Resultados vacíos (modo offline-first): sin resultados del pipeline, la
     * generación opera con placeholders como hasta ahora.
     */
    public static EnterpriseTurnResults empty() {
        return new EnterpriseTurnResults(null, null, null, null, null);
    }
}
