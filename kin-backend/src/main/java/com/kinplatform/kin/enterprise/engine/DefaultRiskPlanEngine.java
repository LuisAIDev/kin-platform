package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.RiskPlanInput;
import com.kinplatform.kin.enterprise.engine.result.RiskPlanResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.RiskMatrix;
import com.kinplatform.kin.enterprise.valueobjects.RiskSeverity;
import com.kinplatform.kin.enterprise.valueobjects.RiskStatus;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación determinista del {@link RiskPlanEngine} (Fase 10,
 * Milestone 2D).
 *
 * <p>Transforma el {@link RiskResult} del pipeline en una {@link RiskMatrix}
 * de presentación: <b>no recalcula el riesgo</b>, únicamente lo convierte a la
 * forma matricial. Reglas funcionales aplicadas:</p>
 *
 * <ul>
 *   <li>La probabilidad e impacto numéricos se derivan del nivel de severidad
 *       del riesgo original: {@code LOW → 0.25}, {@code MEDIUM → 0.50},
 *       {@code HIGH → 0.75}, {@code CRITICAL → 1.00}.</li>
 *   <li>{@code severity} se mapea al {@link RiskSeverity} equivalente.</li>
 *   <li>{@code mitigation} toma la evidencia de la explicación del riesgo
 *       original; {@code owner} queda como {@code "Por definir"} y
 *       {@code status} como {@link RiskStatus#IDENTIFIED}.</li>
 *   <li>Si existe plan financiero y el punto de equilibrio no se alcanza
 *       ({@code breakEvenMonth == 0}), se añade un riesgo financiero derivado
 *       (probabilidad e impacto documentados).</li>
 *   <li>La confianza se hereda del resultado de riesgo original.</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultRiskPlanEngine implements RiskPlanEngine {

    static final String UNDEFINED = "Por definir";

    private static final String ENGINE_NAME = "kin.enterprise:RiskPlan";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 85;

    private static final double LEVEL_LOW = 0.25;
    private static final double LEVEL_MEDIUM = 0.50;
    private static final double LEVEL_HIGH = 0.75;
    private static final double LEVEL_CRITICAL = 1.00;

    private static final double BREAK_EVEN_PROBABILITY = 0.60;
    private static final double BREAK_EVEN_IMPACT = 0.70;

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.FINANCIAL, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public RiskPlanResult evaluate(RiskPlanInput input) {
        if (input == null || input.riskResult() == null) {
            return RiskPlanResult.empty();
        }
        RiskResult riskResult = input.riskResult();

        var risks = new ArrayList<RiskMatrix.Risk>();
        for (var risk : riskResult.risks()) {
            risks.add(toMatrixRisk(risk));
        }
        if (input.financialPlan() != null && !reachesBreakEven(input.financialPlan())) {
            risks.add(breakEvenRisk());
        }

        var matrix = RiskMatrix.of(risks);

        String explanation = buildExplanation(risks.size(),
            riskResult.risks().size(), input.financialPlan());

        return new RiskPlanResult(matrix, riskResult.confidence(), explanation,
            "RiskPlanEngine", ENGINE_VERSION);
    }

    private RiskMatrix.Risk toMatrixRisk(Risk risk) {
        double probability = toLevelValue(risk.severity());
        double impact = toLevelValue(risk.impact());
        RiskSeverity severity = toSeverity(risk.severity());
        String mitigation = evidence(risk);
        return RiskMatrix.Risk.of(probability, impact, severity,
            mitigation, UNDEFINED, RiskStatus.IDENTIFIED);
    }

    private double toLevelValue(RiskLevel level) {
        if (level == null) {
            return LEVEL_LOW;
        }
        return switch (level) {
            case LOW -> LEVEL_LOW;
            case MEDIUM -> LEVEL_MEDIUM;
            case HIGH -> LEVEL_HIGH;
            case CRITICAL -> LEVEL_CRITICAL;
        };
    }

    private RiskSeverity toSeverity(RiskLevel level) {
        if (level == null) {
            return RiskSeverity.LOW;
        }
        return switch (level) {
            case LOW -> RiskSeverity.LOW;
            case MEDIUM -> RiskSeverity.MEDIUM;
            case HIGH -> RiskSeverity.HIGH;
            case CRITICAL -> RiskSeverity.CRITICAL;
        };
    }

    private String evidence(Risk risk) {
        if (risk.explanation() != null && risk.explanation().evidence() != null
            && !risk.explanation().evidence().isBlank()) {
            return risk.explanation().evidence();
        }
        return UNDEFINED;
    }

    private boolean reachesBreakEven(FinancialPlan financialPlan) {
        return financialPlan.breakEvenMonth() > 0;
    }

    private RiskMatrix.Risk breakEvenRisk() {
        return RiskMatrix.Risk.of(BREAK_EVEN_PROBABILITY, BREAK_EVEN_IMPACT,
            RiskSeverity.HIGH, "Revisar la estructura de costes para alcanzar el punto de equilibrio.",
            UNDEFINED, RiskStatus.IDENTIFIED);
    }

    private String buildExplanation(int total, int original, FinancialPlan financialPlan) {
        var sb = new StringBuilder("Matriz de riesgos con " + total + " entradas "
            + "(transformadas de " + original + " riesgos del pipeline).");
        if (financialPlan != null && !reachesBreakEven(financialPlan)) {
            sb.append(" Se añadió el riesgo financiero por falta de punto de equilibrio.");
        }
        return sb.toString();
    }
}
