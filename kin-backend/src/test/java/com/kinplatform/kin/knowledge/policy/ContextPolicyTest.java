package com.kinplatform.kin.knowledge.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextPolicyTest {

    @Test
    void fragmentLimit_excedido_deberiaRechazar() {
        var rule = new FragmentLimitContextRule();

        var decision = rule.evaluate(new ContextBudget(4, 0, 0), ContextPolicyConfig.testing());

        assertTrue(decision.rejected());
        assertEquals(PolicyCategory.CONTEXT, decision.category());
    }

    @Test
    void fragmentLimit_dentro_deberiaPermitir() {
        var rule = new FragmentLimitContextRule();

        assertTrue(rule.evaluate(new ContextBudget(3, 0, 0), ContextPolicyConfig.testing()).allowed());
    }

    @Test
    void fragmentLimit_sinLimite_deberiaPermitir() {
        var rule = new FragmentLimitContextRule();
        var config = new ContextPolicyConfig(0, 0, 0, 0);

        assertTrue(rule.evaluate(new ContextBudget(999, 0, 0), config).allowed());
    }

    @Test
    void tokenBudget_excedido_deberiaRechazar() {
        var rule = new TokenBudgetContextRule();

        var decision = rule.evaluate(new ContextBudget(0, 600, 0), ContextPolicyConfig.testing());

        assertTrue(decision.rejected());
    }

    @Test
    void tokenBudget_dentro_deberiaPermitir() {
        var rule = new TokenBudgetContextRule();

        assertTrue(rule.evaluate(new ContextBudget(0, 500, 0), ContextPolicyConfig.testing()).allowed());
    }

    @Test
    void sizeLimit_excedido_deberiaRechazar() {
        var rule = new ContextSizeLimitContextRule();

        var decision = rule.evaluate(new ContextBudget(0, 0, 3000), ContextPolicyConfig.testing());

        assertTrue(decision.rejected());
    }

    @Test
    void sizeLimit_dentro_deberiaPermitir() {
        var rule = new ContextSizeLimitContextRule();

        assertTrue(rule.evaluate(new ContextBudget(0, 0, 2048), ContextPolicyConfig.testing()).allowed());
    }

    @Test
    void rules_deberianDeclararNombreYCategoria() {
        assertEquals("LimiteFragmentos", new FragmentLimitContextRule().name());
        assertEquals("PresupuestoTokens", new TokenBudgetContextRule().name());
        assertEquals("LimiteTamanoContexto", new ContextSizeLimitContextRule().name());
        assertEquals(PolicyCategory.CONTEXT, new FragmentLimitContextRule().category());
        assertEquals(PolicyCategory.CONTEXT, new TokenBudgetContextRule().category());
        assertEquals(PolicyCategory.CONTEXT, new ContextSizeLimitContextRule().category());
    }

    @Test
    void budget_deberiaAcotarNegativos() {
        var budget = new ContextBudget(-1, -2, -3);

        assertEquals(0, budget.fragmentCount());
        assertEquals(0, budget.estimatedTokens());
        assertEquals(0, budget.estimatedSize());
    }
}
