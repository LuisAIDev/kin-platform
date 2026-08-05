package com.kinplatform.kin.knowledge.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostPolicyTest {

    @Test
    void queryBudget_agotado_deberiaRechazar() {
        var rule = new QueryBudgetCostRule();

        var decision = rule.evaluate(new CostBudgetUsage(2, 0), CostPolicyConfig.testing());

        assertTrue(decision.rejected());
        assertEquals(PolicyCategory.COST, decision.category());
    }

    @Test
    void queryBudget_disponible_deberiaPermitir() {
        var rule = new QueryBudgetCostRule();

        assertTrue(rule.evaluate(new CostBudgetUsage(1, 0), CostPolicyConfig.testing()).allowed());
    }

    @Test
    void queryBudget_sinLimite_deberiaPermitir() {
        var rule = new QueryBudgetCostRule();
        var config = new CostPolicyConfig(0, 0, null);

        assertTrue(rule.evaluate(new CostBudgetUsage(100, 0), config).allowed());
    }

    @Test
    void externalCall_agotado_deberiaRechazar() {
        var rule = new ExternalCallCostRule();

        var decision = rule.evaluate(new CostBudgetUsage(0, 1), CostPolicyConfig.testing());

        assertTrue(decision.rejected());
    }

    @Test
    void externalCall_disponible_deberiaPermitir() {
        var rule = new ExternalCallCostRule();

        assertTrue(rule.evaluate(new CostBudgetUsage(0, 0), CostPolicyConfig.testing()).allowed());
    }

    @Test
    void externalCall_sinLimite_deberiaPermitir() {
        var rule = new ExternalCallCostRule();
        var config = new CostPolicyConfig(0, 0, null);

        assertTrue(rule.evaluate(new CostBudgetUsage(0, 9), config).allowed());
    }

    @Test
    void rules_deberianDeclararNombreYCategoria() {
        assertEquals("PresupuestoConsultas", new QueryBudgetCostRule().name());
        assertEquals("LimiteLlamadasExternas", new ExternalCallCostRule().name());
        assertEquals(PolicyCategory.COST, new QueryBudgetCostRule().category());
        assertEquals(PolicyCategory.COST, new ExternalCallCostRule().category());
    }

    @Test
    void usage_deberiaAcotarNegativos() {
        var usage = new CostBudgetUsage(-5, -2);

        assertEquals(0, usage.consumedQueries());
        assertEquals(0, usage.consumedExternalCalls());
    }
}
