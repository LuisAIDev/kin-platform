package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de riesgos de mercado (MARKET). Evalúa el cliente objetivo,
 * el panorama competitivo y el sector del proyecto.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class MarketRiskAnalyzer implements RiskAnalyzer {

    private static final String VERSION = "v1";

    private final RiskAssembler assembler = new RiskAssembler();

    @Override
    public RiskCategory category() {
        return RiskCategory.MARKET;
    }

    @Override
    public List<Risk> analyze(RiskInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var risks = new ArrayList<Risk>();

        if (!project.isDimensionCovered(AnalyzedDimension.TARGET_CUSTOMER)) {
            risks.add(buildRisk(
                "Cliente objetivo no identificado",
                "Sin un cliente objetivo definido, la estrategia de mercado y comunicación carece de dirección.",
                RiskLevel.HIGH, RiskLevel.HIGH, RiskLevel.HIGH,
                List.of("TARGET_CUSTOMER_NO_IDENTIFICADO"),
                AnalyzedDimension.TARGET_CUSTOMER,
                "Dimensión TARGET_CUSTOMER no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.COMPETITION)) {
            risks.add(buildRisk(
                "Competencia no analizada",
                "Sin análisis competitivo, el proyecto asume supuestos de mercado que pueden ser falsos.",
                RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.MEDIUM,
                List.of("COMPETITION_NO_ANALIZADA"),
                AnalyzedDimension.COMPETITION,
                "Dimensión COMPETITION no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.SECTOR)) {
            risks.add(buildRisk(
                "Sector no caracterizado",
                "Sin conocimiento del sector, es difícil dimensionar el mercado y sus tendencias.",
                RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.MEDIUM,
                List.of("SECTOR_NO_CARACTERIZADO"),
                AnalyzedDimension.SECTOR,
                "Dimensión SECTOR no cubierta",
                evaluation));
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
        var reason = "La dimensión " + dimension.displayName() + " no está cubierta, lo que incrementa el riesgo de mercado.";
        return assembler.build(category(), title, description, severity, probability, impact,
            rules, dimension, reason, evidence, evaluation, version());
    }
}
