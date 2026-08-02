package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import com.kinplatform.kin.knowledge.KnowledgeFact;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de riesgos de negocio (BUSINESS). Evalúa la solidez de la
 * definición estratégica del proyecto: problema, propuesta de valor y objetivos.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class BusinessRiskAnalyzer implements RiskAnalyzer {

    private static final String VERSION = "v1";

    private final RiskAssembler assembler = new RiskAssembler();

    @Override
    public RiskCategory category() {
        return RiskCategory.BUSINESS;
    }

    @Override
    public List<Risk> analyze(RiskInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var risks = new ArrayList<Risk>();

        if (!project.isDimensionCovered(AnalyzedDimension.PROBLEM)) {
            risks.add(buildRisk(
                "Problema no definido",
                "El problema que el proyecto pretende resolver no está claramente definido, lo que compromete la propuesta de valor.",
                RiskLevel.HIGH, RiskLevel.HIGH, RiskLevel.HIGH,
                List.of("PROBLEM_NO_DEFINIDO"),
                AnalyzedDimension.PROBLEM,
                "Dimensión PROBLEM no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.VALUE_PROPOSITION)) {
            risks.add(buildRisk(
                "Propuesta de valor sin definir",
                "Sin una propuesta de valor clara, el proyecto no puede diferenciarse ni comunicar su beneficio.",
                RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH,
                List.of("VALUE_PROPOSITION_NO_DEFINIDA"),
                AnalyzedDimension.VALUE_PROPOSITION,
                "Dimensión VALUE_PROPOSITION no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.OBJECTIVES)) {
            risks.add(buildRisk(
                "Objetivos del proyecto no definidos",
                "Sin objetivos medibles, la ejecución y el seguimiento del proyecto carecen de dirección.",
                RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.MEDIUM,
                List.of("OBJECTIVES_NO_DEFINIDOS"),
                AnalyzedDimension.OBJECTIVES,
                "Dimensión OBJECTIVES no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.SOLUTION)) {
            risks.add(buildRisk(
                "Solución no documentada",
                "La solución propuesta no está documentada, lo que impide evaluar su viabilidad técnica y operativa.",
                RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH,
                List.of("SOLUTION_NO_DOCUMENTADA"),
                AnalyzedDimension.SOLUTION,
                "Dimensión SOLUTION no cubierta",
                evaluation));
        }

        var enrichment = input.enrichment();
        if (enrichment != null && !enrichment.isEmpty()) {
            addEnrichedRisks(enrichment, evaluation, risks);
        }

        return risks;
    }

    @Override
    public String version() {
        return VERSION;
    }

    private Risk buildRisk(String title, String description, RiskLevel severity, RiskLevel probability,
                           RiskLevel impact, List<String> rules, AnalyzedDimension dimension,
                           String evidence, CompletenessEvaluation evaluation) {
        var reason = "La dimensión " + dimension.displayName() + " no está cubierta, lo que incrementa el riesgo de negocio.";
        return assembler.build(category(), title, description, severity, probability, impact,
            rules, dimension, reason, evidence, evaluation, version());
    }

    private void addEnrichedRisks(EnrichmentResult enrichment, CompletenessEvaluation evaluation,
                                  List<Risk> risks) {
        enrichment.rankFor(EvidenceCategory.COMPETITIVE)
            .flatMap(EvidenceRank::top)
            .ifPresent(ev -> risks.add(buildEnrichedRisk(
                "Riesgo de negocio señalado por evidencia competitiva externa",
                "Un hecho verificado indica presión competitiva que conviene mitigar.",
                RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.MEDIUM,
                List.of("ENRIQUECIDO_COMPETITIVO"),
                AnalyzedDimension.COMPETITION,
                evidenceOf(ev),
                evaluation)));
    }

    private Risk buildEnrichedRisk(String title, String description, RiskLevel severity,
                                   RiskLevel probability, RiskLevel impact, List<String> rules,
                                   AnalyzedDimension dimension, String evidence,
                                   CompletenessEvaluation evaluation) {
        var reason = "La evidencia de conocimiento externo verificada sustenta un riesgo de la categoría "
            + category().displayName() + ".";
        return assembler.build(category(), title, description, severity, probability, impact,
            rules, dimension, reason, evidence, evaluation, version());
    }

    private static String evidenceOf(KnowledgeEvidence evidence) {
        var fact = evidence.fact();
        var source = (fact.url() == null || fact.url().isBlank()) ? fact.sourceId() : fact.url();
        return fact.claim() + " (fuente: " + source + ")";
    }
}
