package com.kinplatform.kin.knowledge.policy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderPolicyTest {

    private final ProviderPolicyConfig config = ProviderPolicyConfig.defaults();

    @Test
    void excluded_tipoExcluido_deberiaRechazar() {
        var rule = new ExcludedSourceTypeProviderRule();

        var decision = rule.evaluate("social", ProviderPolicyConfig.production());

        assertTrue(decision.rejected());
        assertFalse(decision.reasons().isEmpty());
    }

    @Test
    void excluded_tipoPermitido_deberiaPermitir() {
        var rule = new ExcludedSourceTypeProviderRule();

        assertTrue(rule.evaluate("government", config).allowed());
    }

    @Test
    void excluded_tipoAusente_deberiaRechazar() {
        var rule = new ExcludedSourceTypeProviderRule();

        assertTrue(rule.evaluate(" ", config).rejected());
        assertTrue(rule.evaluate(null, config).rejected());
    }

    @Test
    void selection_deberiaOrdenarPorPrioridadDescendente() {
        var engine = new KnowledgePolicyEngine();

        var selection = engine.selectProviders(
            Set.of("web_search", "government", "statistics"), config);

        assertEquals(List.of("government", "statistics", "web_search"), selection.allowedTypes());
        assertEquals(5, selection.limit());
    }

    @Test
    void selection_deberiaRespetarLimiteMaximo() {
        var engine = new KnowledgePolicyEngine();

        var selection = engine.selectProviders(
            Set.of("web_search", "government", "document", "vector_rag"), ProviderPolicyConfig.testing());

        assertEquals(2, selection.allowedTypes().size());
        assertEquals("government", selection.allowedTypes().get(0));
        assertTrue(selection.reasons().stream().anyMatch(r -> r.contains("Límite")));
    }

    @Test
    void selection_rechazosPorExclusion_deberianRegistrarse() {
        var engine = new KnowledgePolicyEngine();

        var selection = engine.selectProviders(Set.of("social", "government"), ProviderPolicyConfig.production());

        assertTrue(selection.rejectedTypes().contains("social"));
        assertEquals(List.of("government"), selection.allowedTypes());
    }

    @Test
    void selection_sinLimite_deberiaPermitirTodos() {
        var engine = new KnowledgePolicyEngine();
        var sinLimite = new ProviderPolicyConfig(Set.of(), 0, Map.of());

        var selection = engine.selectProviders(Set.of("a", "b", "c"), sinLimite);

        assertEquals(3, selection.allowedTypes().size());
    }

    @Test
    void selection_entradasNulasYVacias_deberianIgnorarse() {
        var engine = new KnowledgePolicyEngine();
        var candidatos = new HashSet<>(Arrays.asList(" ", null, "government"));

        var selection = engine.selectProviders(candidatos, config);

        assertEquals(1, selection.allowedTypes().size());
        assertEquals(List.of("government"), selection.allowedTypes());
    }

    @Test
    void selection_conjuntoNulo_deberiaVacio() {
        var engine = new KnowledgePolicyEngine();

        var selection = engine.selectProviders(null, config);

        assertTrue(selection.isEmpty());
    }

    @Test
    void selection_sinCandidatosPermitidos_deberiaVacio() {
        var engine = new KnowledgePolicyEngine();

        var selection = engine.selectProviders(Set.of("social", "forum"), ProviderPolicyConfig.production());

        assertTrue(selection.isEmpty());
        assertEquals(2, selection.rejectedTypes().size());
    }

    @Test
    void selection_record_deberiaProtegerListas() {
        var selection = new ProviderSelection(null, null, null, 0);

        assertTrue(selection.allowedTypes().isEmpty());
        assertTrue(selection.rejectedTypes().isEmpty());
        assertTrue(selection.reasons().isEmpty());
        assertEquals(1, selection.limit());
        assertTrue(selection.isEmpty());
    }

    @Test
    void verdict_displayNames() {
        assertEquals("Permitir", PolicyVerdict.ALLOW.displayName());
        assertEquals("Rechazar", PolicyVerdict.REJECT.displayName());
        assertEquals("Degradar", PolicyVerdict.DEGRADE.displayName());
    }
}
