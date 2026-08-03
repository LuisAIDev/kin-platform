package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.risk.RiskResult;

import java.util.UUID;

/**
 * Solicitud de generación del proyecto empresarial (Fase 10, Milestone 2E).
 *
 * <p>Porta la entrada completa del flujo de generación: la identidad del
 * proyecto de KIN origen y los resultados deterministas del pipeline
 * conversacional que consumen los ocho motores del proyecto empresarial
 * ({@code BusinessModelEngine} &hellip; {@code EnterpriseScoreEngine}). El
 * adapter REST (Milestone posterior) construirá esta solicitud a partir de la
 * conversación completada; el dominio no conoce la infraestructura.</p>
 *
 * <p>Invariantes: {@code projectId} y {@code context} son obligatorios. Los
 * resultados del pipeline son opcionales y adoptan su patrón {@code empty()}
 * cuando no se aportan (modo offline-first: los motores operan sin datos
 * externos sin lanzar excepciones).</p>
 *
 * @param projectId      identificador del proyecto de KIN origen
 * @param context        contexto durable del proyecto
 * @param recommendations recomendaciones del pipeline (o {@code null} → vacío)
 * @param opportunities  oportunidades del pipeline (o {@code null} → vacío)
 * @param knowledge      conocimiento externo verificado (o {@code null} → vacío)
 * @param riskResult     riesgo del pipeline (o {@code null} → vacío)
 */
public record EnterpriseGenerationRequest(
    UUID projectId,
    ProjectContext context,
    RecommendationResult recommendations,
    OpportunityResult opportunities,
    KnowledgeResult knowledge,
    RiskResult riskResult
) {

    public EnterpriseGenerationRequest {
        if (projectId == null) {
            throw new IllegalArgumentException("'projectId' de la solicitud de generación no puede ser null.");
        }
        if (context == null) {
            throw new IllegalArgumentException("'context' de la solicitud de generación no puede ser null.");
        }
        recommendations = recommendations == null ? RecommendationResult.empty() : recommendations;
        opportunities = opportunities == null ? OpportunityResult.empty() : opportunities;
        knowledge = knowledge == null ? KnowledgeResult.empty() : knowledge;
        riskResult = riskResult == null ? RiskResult.empty() : riskResult;
    }
}
