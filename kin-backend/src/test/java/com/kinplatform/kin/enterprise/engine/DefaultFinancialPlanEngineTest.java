package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.FinancialPlanInput;
import com.kinplatform.kin.enterprise.engine.result.FinancialPlanResult;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultFinancialPlanEngineTest {

    private final DefaultFinancialPlanEngine engine = new DefaultFinancialPlanEngine();

    @Test
    void metadata_deberiaReportarIdentidadYFase() {
        var metadata = engine.metadata();
        assertEquals("kin.enterprise:FinancialPlan", metadata.name());
        assertEquals(EnginePhase.FINANCIAL, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void evaluate_conInputNull_deberiaRetornarVacio() {
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void evaluate_sinPlanDeMercado_deberiaRetornarVacio() {
        var input = new FinancialPlanInput(
            EngineTestFixtures.contextWithAll(), null,
            EngineTestFixtures.recommendations(0.8));
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_sinContexto_deberiaRetornarVacio() {
        var input = new FinancialPlanInput(null, marketPlan(), null);
        assertTrue(engine.evaluate(input).isEmpty());
    }

    @Test
    void evaluate_deberiaCalcularPlanDeterminista() {
        var input = new FinancialPlanInput(
            EngineTestFixtures.contextWithAll(), marketPlan(),
            EngineTestFixtures.recommendations(0.8));

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        FinancialPlan plan = result.plan();
        assertEquals(250.0, plan.capex());
        assertEquals(400.0, plan.opex());
        assertEquals(1000.0, plan.revenueYear1());
        assertEquals(1100.0, plan.revenueYear2());
        assertEquals(1210.0, plan.revenueYear3());
        assertEquals(5, plan.breakEvenMonth());
        assertEquals(60.0, plan.grossMargin());
        assertEquals(1210.0, plan.base().revenue());
        assertEquals(60.0, plan.base().margin());
        assertEquals(1512.5, plan.optimistic().revenue());
        assertEquals(70.0, plan.optimistic().margin());
        assertEquals(907.5, plan.pessimistic().revenue());
        assertEquals(50.0, plan.pessimistic().margin());
        assertEquals(0.9, result.confidence());
        assertEquals("FinancialPlanEngine", result.generatedBy());
    }

    @Test
    void evaluate_conSomCero_deberiaPuntoDeEquilibrioNoAlcanzado() {
        var market = new MarketPlan(0.0, 0.0, 0.0, 0.0,
            List.of("c"), List.of("ch"), List.of("b"), List.of("s"), 0.5);
        var input = new FinancialPlanInput(
            EngineTestFixtures.contextWithAll(), market,
            EngineTestFixtures.recommendationsEmpty());

        var result = engine.evaluate(input);

        assertFalse(result.isEmpty());
        assertEquals(0, result.plan().breakEvenMonth());
        assertTrue(result.explanation().contains("no alcanza"));
    }

    @Test
    void evaluate_deberiaSerDeterminista() {
        var input = new FinancialPlanInput(
            EngineTestFixtures.contextWithAll(), marketPlan(),
            EngineTestFixtures.recommendations(0.8));

        assertEquals(engine.evaluate(input), engine.evaluate(input));
    }

    private MarketPlan marketPlan() {
        return MarketPlan.of(10000.0, 5000.0, 1000.0, 10.0,
            List.of("Competidor A"), List.of("Directo"), List.of("Regulación"),
            List.of("Retail"), 0.9);
    }
}
