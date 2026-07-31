package com.kinplatform.kin.reporting.risk;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Analizador de riesgos técnicos (TECHNICAL). Evalúa la madurez de la solución,
 * el plan de validación (MVP) y la escalabilidad técnica del proyecto.
 *
 * <p>Servicio de dominio puro: determinista, sin IA y sin infraestructura.</p>
 */
public class TechnicalRiskAnalyzer implements RiskAnalyzer {

    private static final String VERSION = "v1";

    private final RiskAssembler assembler = new RiskAssembler();

    @Override
    public RiskCategory category() {
        return RiskCategory.TECHNICAL;
    }

    @Override
    public List<Risk> analyze(RiskInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var risks = new ArrayList<Risk>();

        if (!project.isDimensionCovered(AnalyzedDimension.MVP)) {
            risks.add(buildRisk(
                "Sin plan de validación técnica (MVP)",
                "No se ha definido un MVP para validar la solución, lo que incrementa el riesgo de construir algo no deseado.",
                RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.HIGH,
                List.of("MVP_NO_DEFINIDO"),
                AnalyzedDimension.MVP,
                "Dimensión MVP no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.SCALABILITY)) {
            risks.add(buildRisk(
                "Escalabilidad técnica sin evaluar",
                "Sin un análisis de escalabilidad, el proyecto puede colapsar ante crecimiento o inversión.",
                RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH,
                List.of("SCALABILITY_NO_EVALUADA"),
                AnalyzedDimension.SCALABILITY,
                "Dimensión SCALABILITY no cubierta",
                evaluation));
        }

        if (!project.isDimensionCovered(AnalyzedDimension.SOLUTION)) {
            risks.add(buildRisk(
                "Detalle técnico de la solución insuficiente",
                "La solución no está descrita con el detalle necesario para evaluar su factibilidad técnica.",
                RiskLevel.MEDIUM, RiskLevel.LOW, RiskLevel.MEDIUM,
                List.of("SOLUTION_SIN_DETALLE"),
                AnalyzedDimension.SOLUTION,
                "Dimensión SOLUTION no cubierta",
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
        var reason = "La dimensión " + dimension.displayName() + " no está cubierta, lo que incrementa el riesgo técnico.";
        return assembler.build(category(), title, description, severity, probability, impact,
            rules, dimension, reason, evidence, evaluation, version());
    }
}
