package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.EnterpriseScoreInput;
import com.kinplatform.kin.enterprise.engine.result.EnterpriseScoreResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.InnovationLevel;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.enterprise.valueobjects.ScoreGrade;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultEnterpriseScoreEngineTest {

    private final DefaultEnterpriseScoreEngine engine = new DefaultEnterpriseScoreEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:EnterpriseScore", metadata.name());
        assertEquals(EnginePhase.SCORING, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_conContextoNull_deberiaRetornarVacio() {
        var input = new EnterpriseScoreInput(null, null, null, null, null, null, null, null,
            null, null, null, null);
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_conDatosParciales_deberiaCalcularConCeros() {
        var input = new EnterpriseScoreInput(
            EngineTestFixtures.contextWithAll(), null, null, null, null, null, null, null,
            null, null, null, null);

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        assertEquals(0.0, result.score().market());
        assertEquals(0.0, result.score().financial());
        assertEquals(0.0, result.confidence());
        assertTrue(result.explanation().contains("faltan datos"));
        assertEquals("EnterpriseScoreEngine", result.generatedBy());
    }

    @Test
    void evaluate_conDatosCompletos_deberiaCalcularOchoDimensiones() {
        var input = fullInput();

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        assertEquals(36.666666666666664, result.score().market(), 0.001);
        assertEquals(75.0, result.score().innovation(), 0.001);
        assertEquals(64.28571428571429, result.score().viability(), 0.001);
        assertEquals(69.58333333333333, result.score().financial(), 0.001);
        assertEquals(36.0, result.score().risk(), 0.001);
        assertEquals(70.0, result.score().scalability(), 0.001);
        assertEquals(70.0, result.score().team(), 0.001);
        assertEquals(80.0, result.score().sustainability(), 0.001);
        assertEquals(63, result.score().overallScore());
        assertEquals(ScoreGrade.FAIR, result.score().grade());
        assertEquals(0.82, result.confidence(), 0.0001);
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        assertEquals(engine.evaluate(fullInput()), engine.evaluate(fullInput()));
    }

    @Test
    void evaluate_conNivelDisruptivoYriesgoBajo_deberiaPuntuarAlto() {
        var market = MarketPlan.of(10000.0, 5000.0, 1000.0, 10.0,
            List.of("c"), List.of("ch"), List.of("b"), List.of("s"), 1.0);
        var innovation = InnovationPlan.of(InnovationLevel.DISRUPTIVE,
            List.of("Diferenciador"), "Defensa", List.of("I+D"), List.of());
        var risk = lowRiskResult();

        var input = new EnterpriseScoreInput(
            EngineTestFixtures.contextWithAll(), null, market, innovation, financialPlan(),
            null, null, null, null, null, EngineTestFixtures.knowledge(1.0), risk);

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        assertEquals(95.0, result.score().innovation(), 0.001);
        assertTrue(result.score().risk() > 50.0);
        assertEquals(1.0, result.confidence(), 0.001);
    }

    private RiskResult lowRiskResult() {
        var risk = Risk.create(RiskCategory.MARKET, "Riesgo bajo", "Descripción",
            RiskLevel.LOW, RiskLevel.LOW, RiskLevel.LOW, 1.0,
            RiskExplanation.of(List.of("dato"), "regla", "razón", "evidencia"),
            List.of("r1"), com.kinplatform.kin.context.AnalyzedDimension.COMPETITION, "1.0.0");
        return new RiskResult(List.of(risk), RiskLevel.LOW, List.of(risk), 1.0,
            "Riesgos bajos.", "RiskEngine", "1.0.0");
    }

    private EnterpriseScoreInput fullInput() {
        return new EnterpriseScoreInput(
            EngineTestFixtures.contextWithAll(), null,
            MarketPlan.of(10000.0, 5000.0, 1000.0, 10.0,
                List.of("c"), List.of("ch"), List.of("b"), List.of("s"), 0.9),
            InnovationPlan.of(InnovationLevel.TRANSFORMATIONAL,
                List.of("Diferenciador"), "Defensa", List.of("I+D"), List.of()),
            financialPlan(), null, null, null,
            EngineTestFixtures.recommendations(0.8),
            EngineTestFixtures.opportunities(0.8),
            EngineTestFixtures.knowledge(0.8),
            EngineTestFixtures.riskResult(0.8));
    }

    private FinancialPlan financialPlan() {
        return FinancialPlan.of(250.0, 400.0, 1000.0, 1100.0, 1210.0, 5, 60.0,
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0));
    }
}
