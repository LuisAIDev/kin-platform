package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.engine.EngineResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeResultTest {

    @Test
    void empty_deberiaEstarVacia() {
        var result = KnowledgeResult.empty();

        assertTrue(result.isEmpty());
        assertTrue(result.facts().isEmpty());
        assertTrue(result.sourcesUsed().isEmpty());
        assertTrue(result.validations().isEmpty());
        assertEquals(0.0, result.confidence());
        assertEquals(0, result.factCount());
    }

    @Test
    void constructor_deberiaExponerCampos() {
        var fact = KnowledgeFact.of("claim", "s1", "u", null, SourceTrust.SECONDARY, "C");
        var validation = SourceValidation.accepted(SourceTrust.OFFICIAL_PUBLIC);
        var result = new KnowledgeResult(
            List.of(fact), List.of("s1"), List.of(validation), 0.8,
            "explicación", "KnowledgeEngine", "1.0");

        assertEquals(List.of(fact), result.facts());
        assertEquals(List.of("s1"), result.sourcesUsed());
        assertEquals(List.of(validation), result.validations());
        assertEquals(0.8, result.confidence());
        assertEquals("explicación", result.explanation());
        assertEquals("KnowledgeEngine", result.generatedBy());
        assertEquals("1.0", result.engineVersion());
        assertEquals(1, result.factCount());
    }

    @Test
    void isEmpty_deberiaReflejarLosHechos() {
        var result = new KnowledgeResult(List.of(), List.of(), List.of(), 0.9, "e", "k", "1.0");
        assertTrue(result.isEmpty());

        var fact = KnowledgeFact.of("claim", "s1", "u", null, SourceTrust.SECONDARY, "C");
        var withFacts = new KnowledgeResult(List.of(fact), List.of(), List.of(), 0.9, "e", "k", "1.0");
        assertTrue(!withFacts.isEmpty());
    }

    @Test
    void constructor_deberiaAcotarLaConfianza() {
        assertEquals(1.0, new KnowledgeResult(List.of(), List.of(), List.of(), 1.5, "e", "k", "1.0").confidence());
        assertEquals(0.0, new KnowledgeResult(List.of(), List.of(), List.of(), -0.2, "e", "k", "1.0").confidence());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var result = new KnowledgeResult(null, null, null, 0.0, null, null, null);

        assertTrue(result.isEmpty());
        assertTrue(result.facts().isEmpty());
        assertTrue(result.sourcesUsed().isEmpty());
        assertTrue(result.validations().isEmpty());
        assertEquals("", result.explanation());
        assertEquals("", result.generatedBy());
        assertEquals("", result.engineVersion());
    }

    @Test
    void constructor_deberiaProtegerListas() {
        var facts = new ArrayList<>(List.of(
            KnowledgeFact.of("claim", "s1", "u", null, SourceTrust.SECONDARY, "C")));
        var result = new KnowledgeResult(facts, List.of(), List.of(), 0.5, "e", "k", "1.0");

        facts.clear();
        assertThrows(UnsupportedOperationException.class,
            () -> result.facts().add(KnowledgeFact.of("x", "s", "u", null, SourceTrust.UNVERIFIED, "C")));
        assertEquals(1, result.facts().size());
    }

    @Test
    void deberiaImplementarEngineResult() {
        assertTrue(EngineResult.class.isAssignableFrom(KnowledgeResult.class));
    }
}
