package com.kinplatform.kin.reporting.opportunity;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de oportunidades de mercado (MERCADO). Detecta brechas en sector,
 * cliente objetivo y problema que representan oportunidades de validación y
 * expansión del mercado.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class MarketOpportunityAnalyzer implements OpportunityAnalyzer {

    private static final String VERSION = "v1";

    private final OpportunityAssembler assembler = new OpportunityAssembler();

    @Override
    public OpportunityCategory category() {
        return OpportunityCategory.MERCADO;
    }

    @Override
    public List<Opportunity> analyze(OpportunityInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var priorityFromScore = assembler.computePriorityFromScore(input.score().totalScore());
        var opportunities = new ArrayList<Opportunity>();

        if (!project.isDimensionCovered(AnalyzedDimension.TARGET_CUSTOMER)) {
            opportunities.add(buildOpportunity(
                    "Segmentar el cliente objetivo",
                    "Definir con precisión el cliente objetivo permitirá orientar la propuesta de valor y la comunicación.",
                    priorityFromScore,
                    0,
                    assembler.missingBonus(evaluation, AnalyzedDimension.TARGET_CUSTOMER),
                    ImpactLevel.HIGH,
                    EffortLevel.MEDIUM,
                    List.of("TARGET_CUSTOMER_NO_CUBIERTO"),
                    AnalyzedDimension.TARGET_CUSTOMER,
                    "Dimensión TARGET_CUSTOMER no cubierta",
                    evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.SECTOR)) {
            opportunities.add(buildOpportunity(
                    "Caracterizar el sector y sus tendencias",
                    "Documentar el sector permite dimensionar el mercado y detectar tendencias aprovechables.",
                    priorityFromScore,
                    0,
                    assembler.missingBonus(evaluation, AnalyzedDimension.SECTOR),
                    ImpactLevel.MEDIUM,
                    EffortLevel.LOW,
                    List.of("SECTOR_NO_CUBIERTO"),
                    AnalyzedDimension.SECTOR,
                    "Dimensión SECTOR no cubierta",
                    evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.PROBLEM)) {
            opportunities.add(buildOpportunity(
                    "Precisar el problema que resuelve",
                    "Un problema bien definido facilita la validación con clientes y la comunicación de valor.",
                    priorityFromScore,
                    0,
                    assembler.missingBonus(evaluation, AnalyzedDimension.PROBLEM),
                    ImpactLevel.HIGH,
                    EffortLevel.MEDIUM,
                    List.of("PROBLEM_NO_CUBIERTO"),
                    AnalyzedDimension.PROBLEM,
                    "Dimensión PROBLEM no cubierta",
                    evaluation));
        }

        if (assembler.hasSignal(evaluation, "mercado")) {
            opportunities.add(buildOpportunity(
                    "Explorar la señal de oportunidad de mercado",
                    "La evaluación detectó una señal de oportunidad de mercado que conviene capitalizar.",
                    priorityFromScore,
                    2,
                    0,
                    ImpactLevel.HIGH,
                    EffortLevel.MEDIUM,
                    List.of("SEÑAL_MERCADO_DETECTADA"),
                    AnalyzedDimension.SECTOR,
                    "Señal detectada por la evaluación de completitud",
                    evaluation));
        }

        var enrichment = input.enrichment();
        if (enrichment != null && !enrichment.isEmpty()) {
            addEnrichedOpportunities(enrichment, priorityFromScore, evaluation, opportunities);
        }

        return opportunities;
    }

    @Override
    public String version() {
        return VERSION;
    }

    private Opportunity buildOpportunity(
            String title,
            String description,
            int priorityFromScore,
            int detectedBonus,
            int missingBonus,
            ImpactLevel impact,
            EffortLevel effort,
            List<String> rules,
            AnalyzedDimension dimension,
            String evidence,
            CompletenessEvaluation evaluation) {
        var reason =
                "La dimensión " + dimension.displayName() + " o una señal detectada abren una oportunidad de mercado.";
        return assembler.build(
                category(),
                title,
                description,
                priorityFromScore,
                missingBonus,
                detectedBonus,
                impact,
                effort,
                rules,
                dimension,
                reason,
                evidence,
                evaluation,
                version());
    }

    private void addEnrichedOpportunities(
            EnrichmentResult enrichment,
            int priorityFromScore,
            CompletenessEvaluation evaluation,
            List<Opportunity> opportunities) {
        enrichment
                .rankFor(EvidenceCategory.MARKET)
                .flatMap(EvidenceRank::top)
                .ifPresent(ev -> opportunities.add(buildEnrichedOpportunity(
                        "Validar la tendencia de mercado con evidencia externa",
                        "Un hecho verificado respalda una señal de mercado que conviene capitalizar.",
                        priorityFromScore,
                        ImpactLevel.HIGH,
                        EffortLevel.MEDIUM,
                        List.of("ENRIQUECIDO_MERCADO"),
                        AnalyzedDimension.SECTOR,
                        evidenceOf(ev),
                        evaluation)));
    }

    private Opportunity buildEnrichedOpportunity(
            String title,
            String description,
            int priorityFromScore,
            ImpactLevel impact,
            EffortLevel effort,
            List<String> rules,
            AnalyzedDimension dimension,
            String evidence,
            CompletenessEvaluation evaluation) {
        var reason = "La evidencia de conocimiento externo verificada sustenta una oportunidad de la categoría "
                + category().displayName() + ".";
        return assembler.build(
                category(),
                title,
                description,
                priorityFromScore,
                0,
                2,
                impact,
                effort,
                rules,
                dimension,
                reason,
                evidence,
                evaluation,
                version());
    }

    private static String evidenceOf(KnowledgeEvidence evidence) {
        var fact = evidence.fact();
        var source = (fact.url() == null || fact.url().isBlank()) ? fact.sourceId() : fact.url();
        return fact.claim() + " (fuente: " + source + ")";
    }
}
