package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.RiskPlanInput;
import com.kinplatform.kin.enterprise.engine.result.RiskPlanResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.RiskMatrix;
import com.kinplatform.kin.enterprise.valueobjects.RiskSeverity;
import com.kinplatform.kin.enterprise.valueobjects.RiskStatus;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRiskPlanEngineTest {

    private final DefaultRiskPlanEngine engine = new DefaultRiskPlanEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:RiskPlan", metadata.name());
        assertEquals(EnginePhase.FINANCIAL, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_sinRiesgos_deberiaRetornarVacio() {
        var input = new RiskPlanInput(null, financialPlan(5));
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_deberiaTransformarRiesgosASuFormaMatricial() {
        var input = new RiskPlanInput(
            EngineTestFixtures.riskResult(0.8), financialPlan(5));

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        RiskMatrix matrix = result.matrix();
        assertEquals(1, matrix.risks().size());
        RiskMatrix.Risk matrixRisk = matrix.risks().get(0);
        assertEquals(0.75, matrixRisk.probability());
        assertEquals(0.75, matrixRisk.impact());
        assertEquals(RiskSeverity.HIGH, matrixRisk.severity());
        assertEquals("evidencia", matrixRisk.mitigation());
        assertEquals("Por definir", matrixRisk.owner());
        assertEquals(RiskStatus.IDENTIFIED, matrixRisk.status());
        assertEquals(0.8, result.confidence());
        assertEquals("RiskPlanEngine", result.generatedBy());
    }

    @Test
    void evaluate_sinPuntoDeEquilibrio_deberiaAnadirRiesgoFinanciero() {
        var input = new RiskPlanInput(
            EngineTestFixtures.riskResult(0.8), financialPlan(0));

        var result = engine.evaluate(input);

        RiskMatrix matrix = result.matrix();
        assertEquals(2, matrix.risks().size());
        assertTrue(result.explanation().contains("financiero"));
    }

    @Test
    void evaluate_sinPlanFinanciero_deberiaUsarSoloRiesgos() {
        var input = new RiskPlanInput(EngineTestFixtures.riskResult(0.8), null);

        var result = engine.evaluate(input);

        assertEquals(1, result.matrix().risks().size());
    }

    @Test
    void evaluate_sinEvidencia_deberiaMitigacionPorDefinir() {
        var risk = Risk.create(RiskCategory.MARKET, "Riesgo de mercado", "Descripción",
            RiskLevel.MEDIUM, RiskLevel.LOW, RiskLevel.MEDIUM, 0.6,
            RiskExplanation.of(List.of(), "regla", "razón", null),
            List.of("r1"), AnalyzedDimension.COMPETITION, "1.0.0");
        var riskResult = new RiskResult(List.of(risk), RiskLevel.MEDIUM, List.of(risk), 0.6,
            "Riesgos.", "RiskEngine", "1.0.0");
        var input = new RiskPlanInput(riskResult, financialPlan(5));

        var result = engine.evaluate(input);

        RiskMatrix.Risk matrixRisk = result.matrix().risks().get(0);
        assertEquals(0.5, matrixRisk.probability());
        assertEquals(0.5, matrixRisk.impact());
        assertEquals(RiskSeverity.MEDIUM, matrixRisk.severity());
        assertEquals("Por definir", matrixRisk.mitigation());
    }

    @Test
    void evaluate_conNivelCritico_deberiaMapearACero() {
        var risk = Risk.create(RiskCategory.TECHNICAL, "Riesgo crítico", "Descripción",
            RiskLevel.CRITICAL, RiskLevel.CRITICAL, RiskLevel.CRITICAL, 0.9,
            RiskExplanation.of(List.of(), "regla", "razón", "evidencia"),
            List.of("r1"), AnalyzedDimension.SOLUTION, "1.0.0");
        var riskResult = new RiskResult(List.of(risk), RiskLevel.CRITICAL, List.of(risk), 0.9,
            "Riesgos.", "RiskEngine", "1.0.0");
        var input = new RiskPlanInput(riskResult, financialPlan(5));

        var result = engine.evaluate(input);

        RiskMatrix.Risk matrixRisk = result.matrix().risks().get(0);
        assertEquals(1.0, matrixRisk.probability());
        assertEquals(1.0, matrixRisk.impact());
        assertEquals(RiskSeverity.CRITICAL, matrixRisk.severity());
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new RiskPlanInput(
            EngineTestFixtures.riskResult(0.8), financialPlan(5));

        assertEquals(engine.evaluate(input), engine.evaluate(input));
    }

    private FinancialPlan financialPlan(int breakEvenMonth) {
        return FinancialPlan.of(250.0, 400.0, 1000.0, 1100.0, 1210.0, breakEvenMonth, 60.0,
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0));
    }
}
