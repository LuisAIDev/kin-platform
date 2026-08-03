package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.InnovationInput;
import com.kinplatform.kin.enterprise.engine.result.InnovationResult;
import com.kinplatform.kin.enterprise.valueobjects.InnovationLevel;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación determinista del {@link InnovationEngine} (Fase 10,
 * Milestone 2D).
 *
 * <p>Construye el {@link InnovationPlan} reutilizando el
 * {@link OpportunityResult} del pipeline: <b>no recalcula el análisis de
 * oportunidades</b>, únicamente lo transforma en el plan de innovación. Reglas
 * funcionales aplicadas:</p>
 *
 * <ul>
 *   <li>{@code innovationLevel} &larr; derivado de las oportunidades de
 *       innovación y tecnológicas: 3 o más con prioridad &ge; 7 implica
 *       {@link InnovationLevel#DISRUPTIVE}; al menos una
 *       {@link InnovationLevel#TRANSFORMATIONAL}; si no,
 *       {@link InnovationLevel#INCREMENTAL}.</li>
 *   <li>{@code differentiators} &larr; títulos de las oportunidades
 *       prioritarias de innovación/tecnología/competencia.</li>
 *   <li>{@code defensibility} &larr; oportunidad competitiva o
 *       {@code "Sin definir"}.</li>
 *   <li>{@code innovationRoadmap} &larr; títulos de las oportunidades de
 *       innovación y tecnológicas.</li>
 *   <li>{@code researchRecommendations} &larr; hechos del conocimiento externo
 *       cuando existen (recomendaciones de investigación); lista vacía si no.</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultInnovationEngine implements InnovationEngine {

    private static final String ENGINE_NAME = "kin.enterprise:Innovation";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 82;

    private static final int DISRUPTIVE_MIN_COUNT = 3;
    private static final int DISRUPTIVE_MIN_PRIORITY = 7;

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.INNOVATION, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public InnovationResult evaluate(InnovationInput input) {
        if (input == null || input.context() == null) {
            return InnovationResult.empty();
        }
        var opportunities = input.opportunities() == null ? OpportunityResult.empty() : input.opportunities();

        InnovationLevel level = deriveLevel(opportunities);
        List<String> differentiators = differentiators(opportunities);
        String defensibility = defensibility(opportunities);
        List<String> roadmap = innovationRoadmap(opportunities);
        List<String> research = researchRecommendations(input.knowledge());

        var plan = InnovationPlan.of(level, differentiators, defensibility, roadmap, research);

        String explanation = buildExplanation(opportunities);

        return new InnovationResult(plan, opportunities.confidence(), explanation,
            "InnovationEngine", ENGINE_VERSION);
    }

    private InnovationLevel deriveLevel(OpportunityResult opportunities) {
        long count = opportunities.opportunities().stream()
            .filter(this::isInnovationOpportunity)
            .count();
        int maxPriority = opportunities.opportunities().stream()
            .filter(this::isInnovationOpportunity)
            .mapToInt(Opportunity::priority)
            .max()
            .orElse(0);
        if (count >= DISRUPTIVE_MIN_COUNT && maxPriority >= DISRUPTIVE_MIN_PRIORITY) {
            return InnovationLevel.DISRUPTIVE;
        }
        if (count >= 1) {
            return InnovationLevel.TRANSFORMATIONAL;
        }
        return InnovationLevel.INCREMENTAL;
    }

    private boolean isInnovationOpportunity(Opportunity opportunity) {
        return opportunity.category() == OpportunityCategory.INNOVACION
            || opportunity.category() == OpportunityCategory.TECNOLOGICA;
    }

    private List<String> differentiators(OpportunityResult opportunities) {
        var list = new ArrayList<String>();
        for (var opportunity : opportunities.topOpportunities()) {
            var category = opportunity.category();
            if ((category == OpportunityCategory.INNOVACION
                || category == OpportunityCategory.TECNOLOGICA
                || category == OpportunityCategory.COMPETITIVA)
                && opportunity.title() != null && !opportunity.title().isBlank()) {
                list.add(opportunity.title());
            }
        }
        return List.copyOf(list);
    }

    private String defensibility(OpportunityResult opportunities) {
        for (var opportunity : opportunities.opportunities()) {
            if (opportunity.category() == OpportunityCategory.COMPETITIVA
                && opportunity.title() != null && !opportunity.title().isBlank()) {
                return opportunity.title();
            }
        }
        return InnovationPlan.empty().defensibility();
    }

    private List<String> innovationRoadmap(OpportunityResult opportunities) {
        var list = new ArrayList<String>();
        for (var opportunity : opportunities.opportunities()) {
            if (isInnovationOpportunity(opportunity)
                && opportunity.title() != null && !opportunity.title().isBlank()) {
                list.add(opportunity.title());
            }
        }
        return List.copyOf(list);
    }

    private List<String> researchRecommendations(KnowledgeResult knowledge) {
        if (knowledge == null) {
            return List.of();
        }
        var list = new ArrayList<String>();
        for (var fact : knowledge.facts()) {
            if (fact.claim() != null && !fact.claim().isBlank()) {
                list.add(fact.claim());
            }
        }
        return List.copyOf(list);
    }

    private String buildExplanation(OpportunityResult opportunities) {
        if (opportunities.isEmpty()) {
            return "Sin oportunidades previas: plan de innovación incremental por defecto.";
        }
        return "Plan de innovación derivado de " + opportunities.opportunityCount()
            + " oportunidades previas.";
    }
}
