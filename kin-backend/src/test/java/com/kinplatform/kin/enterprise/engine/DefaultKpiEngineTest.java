package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.KpiInput;
import com.kinplatform.kin.enterprise.engine.result.KpiResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.KpiSet;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultKpiEngineTest {

    private final DefaultKpiEngine engine = new DefaultKpiEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:Kpi", metadata.name());
        assertEquals(EnginePhase.FINANCIAL, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_sinPlanDeMercado_deberiaRetornarVacio() {
        var input = new KpiInput(
            EngineTestFixtures.contextWithAll(), null, financialPlan());
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_sinPlanFinanciero_deberiaRetornarVacio() {
        var input = new KpiInput(
            EngineTestFixtures.contextWithAll(), marketPlan(), null);
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_deberiaDefinirCincoFases() {
        var input = new KpiInput(
            EngineTestFixtures.contextWithAll(), marketPlan(), financialPlan());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        KpiSet kpis = result.kpis();
        assertEquals(1, kpis.acquisition().size());
        assertEquals(1, kpis.activation().size());
        assertEquals(1, kpis.retention().size());
        assertEquals(1, kpis.revenue().size());
        assertEquals(1, kpis.financial().size());
        assertEquals(1000.0, kpis.acquisition().get(0).target());
        assertEquals(0.30, kpis.activation().get(0).target());
        assertEquals(0.80, kpis.retention().get(0).target());
        assertEquals(1210.0, kpis.revenue().get(0).target());
        assertEquals(60.0, kpis.financial().get(0).target());
        assertEquals(0.9, result.confidence());
        assertEquals("KpiEngine", result.generatedBy());
        assertEquals("1.0.0", result.engineVersion());
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new KpiInput(
            EngineTestFixtures.contextWithAll(), marketPlan(), financialPlan());

        assertEquals(engine.evaluate(input), engine.evaluate(input));
    }

    private MarketPlan marketPlan() {
        return MarketPlan.of(10000.0, 5000.0, 1000.0, 10.0,
            List.of("c"), List.of("ch"), List.of("b"), List.of("s"), 0.9);
    }

    private FinancialPlan financialPlan() {
        return FinancialPlan.of(250.0, 400.0, 1000.0, 1100.0, 1210.0, 5, 60.0,
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0),
            FinancialPlan.Scenario.of(1210.0, 60.0));
    }
}
