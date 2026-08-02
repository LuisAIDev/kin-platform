package com.kinplatform.kin.interview;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerRulesTest {

    @Test
    void defaults_deberiaExponerReglasPorDefecto() {
        var rules = AnswerRules.defaults();

        assertEquals(0, rules.minLength());
        assertTrue(rules.minKeywords().isEmpty());
        assertEquals("", rules.requiredFormat());
        assertTrue(rules.allowRefinement());
        assertEquals(1, rules.maxRefinements());
        assertFalse(rules.hasKeywordRequirements());
        assertFalse(rules.hasFormatRequirement());
    }

    @Test
    void of_deberiaExponerCampos() {
        var rules = AnswerRules.of(10, false, 2);

        assertEquals(10, rules.minLength());
        assertFalse(rules.allowRefinement());
        assertEquals(2, rules.maxRefinements());
        assertTrue(rules.minKeywords().isEmpty());
        assertEquals("", rules.requiredFormat());
    }

    @Test
    void constructor_deberiaExponerCamposCompletos() {
        var rules = new AnswerRules(5, List.of("mercado", "clientes"), "párrafo", true, 3);

        assertEquals(5, rules.minLength());
        assertEquals(List.of("mercado", "clientes"), rules.minKeywords());
        assertEquals("párrafo", rules.requiredFormat());
        assertTrue(rules.allowRefinement());
        assertEquals(3, rules.maxRefinements());
        assertTrue(rules.hasKeywordRequirements());
        assertTrue(rules.hasFormatRequirement());
    }

    @Test
    void constructor_deberiaAcotarValoresNegativos() {
        var rules = new AnswerRules(-5, List.of(), "", true, -1);

        assertEquals(0, rules.minLength());
        assertEquals(0, rules.maxRefinements());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var rules = new AnswerRules(0, null, null, true, 0);

        assertTrue(rules.minKeywords().isEmpty());
        assertEquals("", rules.requiredFormat());
        assertFalse(rules.hasKeywordRequirements());
        assertFalse(rules.hasFormatRequirement());
    }

    @Test
    void constructor_deberiaProtegerListaDeKeywords() {
        var keywords = new ArrayList<>(List.of("mercado"));
        var rules = new AnswerRules(0, keywords, "", true, 0);

        keywords.clear();
        assertEquals(1, rules.minKeywords().size());
        assertThrows(UnsupportedOperationException.class,
            () -> rules.minKeywords().add("otro"));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = AnswerRules.of(10, true, 2);
        var b = AnswerRules.of(10, true, 2);
        var c = AnswerRules.of(20, true, 2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("minLength=10"));
    }
}
