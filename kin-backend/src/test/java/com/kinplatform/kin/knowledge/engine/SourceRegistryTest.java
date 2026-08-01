package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceRegistryTest {

    private final KnowledgeSource sourceA = query -> List.of();
    private final KnowledgeSource sourceB = query -> List.of();
    private final KnowledgeSource sourceC = query -> List.of();

    @Test
    void constructor_deberiaConservarOrdenYDescartarNulos() {
        var registry = new SourceRegistry(Arrays.asList(sourceA, null, sourceB));

        assertEquals(List.of(sourceA, sourceB), registry.all());
        assertEquals(2, registry.size());
    }

    @Test
    void constructor_deberiaAceptarListaNula() {
        var registry = new SourceRegistry(null);

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
    }

    @Test
    void register_deberiaAgregarFuente() {
        var registry = new SourceRegistry(List.of());
        registry.register(sourceA);
        registry.register(sourceB);

        assertEquals(2, registry.size());
        assertEquals(List.of(sourceA, sourceB), registry.all());
    }

    @Test
    void register_deberiaIgnorarNulo() {
        var registry = new SourceRegistry(List.of());
        registry.register(null);

        assertTrue(registry.isEmpty());
    }

    @Test
    void register_deberiaEvitarDuplicados() {
        var registry = new SourceRegistry(List.of(sourceA));
        registry.register(sourceA);

        assertEquals(1, registry.size());
    }

    @Test
    void empty_deberiaEstarVacia() {
        var registry = SourceRegistry.empty();

        assertTrue(registry.isEmpty());
        assertTrue(registry.all().isEmpty());
    }

    @Test
    void all_deberiaSerInmutable() {
        var registry = new SourceRegistry(List.of(sourceA));

        assertThrows(UnsupportedOperationException.class,
            () -> registry.all().add(sourceB));
        assertEquals(1, registry.all().size());
    }

    @Test
    void all_deberiaReflejarElRegistroActual() {
        var registry = new SourceRegistry(List.of(sourceA));
        registry.register(sourceB);

        assertTrue(registry.all().contains(sourceB));
        assertFalse(registry.isEmpty());
    }
}
