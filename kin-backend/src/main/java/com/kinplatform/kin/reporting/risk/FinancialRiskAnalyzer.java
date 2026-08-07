package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.enrichment.EvidenceRank;
import com.kinplatform.kin.enrichment.KnowledgeEvidence;
import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de riesgos financieros (FINANCIAL). Evalúa el modelo de ingresos,
 * los recursos disponibles y la salud financiera general según el score.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class FinancialRiskAnalyzer implements RiskAnalyzer {

    private static final String VERSION = "v1";

    private final RiskAssembler assembler = new RiskAssembler();

    @Override
    public RiskCategory category() {
        return RiskCategory.FINANCIAL;
    }

    @Override
    public List<Risk> analyze(RiskInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var score = input.score();
        var risks = new ArrayList<Risk>();

        if (!project.isDimensionCovered(AnalyzedDimension.REVENUE_MODEL)) {
            risks.add(buildRisk(
                    "Modelo de ingresos no definido",
                    "Sin modelo de ingresos el proyecto no puede sostenerse financieramente.",
                    RiskLevel.CRITICAL,
                    RiskLevel.HIGH,
                    RiskLevel.CRITICAL,
                    List.of("REVENUE_MODEL_NO_DEFINIDO"),
                    AnalyzedDimension.REVENUE_MODEL,
                    "Dimensión REVENUE_MODEL no cubierta",
                    evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.RESOURCES)) {
            risks.add(buildRisk(
                    "Recursos financieros no planificados",
                    "No se han definido los recursos necesarios, lo que puede detener la ejecución del proyecto.",
                    RiskLevel.MEDIUM,
                    RiskLevel.MEDIUM,
                    RiskLevel.MEDIUM,
                    List.of("RESOURCES_NO_PLANIFICADOS"),
                    AnalyzedDimension.RESOURCES,
                    "Dimensión RESOURCES no cubierta",
                    evaluation));
        }

        if (score.totalScore() < 40) {
            risks.add(buildRisk(
                    "Salud financiera general débil",
                    "El score global del proyecto es bajo, lo que señala debilidad financiera en varios frentes.",
                    RiskLevel.HIGH,
                    RiskLevel.MEDIUM,
                    RiskLevel.HIGH,
                    List.of("SCORE_GLOBAL_BAJO"),
                    AnalyzedDimension.REVENUE_MODEL,
                    "Score total: " + score.totalScore() + "/" + score.maxScore(),
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

    private Risk buildRisk(
            String title,
            String description,
            RiskLevel severity,
            RiskLevel probability,
            RiskLevel impact,
            List<String> rules,
            AnalyzedDimension dimension,
            String evidence,
            CompletenessEvaluation evaluation) {
        var reason = "La dimensión " + dimension.displayName() + " o el score global indican debilidad financiera.";
        return assembler.build(
                category(),
                title,
                description,
                severity,
                probability,
                impact,
                rules,
                dimension,
                reason,
                evidence,
                evaluation,
                version());
    }

    private void addEnrichedRisks(EnrichmentResult enrichment, CompletenessEvaluation evaluation, List<Risk> risks) {
        enrichment
                .rankFor(EvidenceCategory.FINANCIAL)
                .flatMap(EvidenceRank::top)
                .ifPresent(ev -> risks.add(buildEnrichedRisk(
                        "Riesgo financiero señalado por evidencia externa",
                        "Un hecho verificado señala una condición financiera que conviene mitigar.",
                        RiskLevel.MEDIUM,
                        RiskLevel.MEDIUM,
                        RiskLevel.MEDIUM,
                        List.of("ENRIQUECIDO_FINANCIERO"),
                        AnalyzedDimension.REVENUE_MODEL,
                        evidenceOf(ev),
                        evaluation)));
    }

    private Risk buildEnrichedRisk(
            String title,
            String description,
            RiskLevel severity,
            RiskLevel probability,
            RiskLevel impact,
            List<String> rules,
            AnalyzedDimension dimension,
            String evidence,
            CompletenessEvaluation evaluation) {
        var reason = "La evidencia de conocimiento externo verificada sustenta un riesgo de la categoría "
                + category().displayName() + ".";
        return assembler.build(
                category(),
                title,
                description,
                severity,
                probability,
                impact,
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
