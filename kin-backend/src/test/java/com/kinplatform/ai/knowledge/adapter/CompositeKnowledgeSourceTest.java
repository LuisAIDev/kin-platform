package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeKnowledgeSourceTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private KnowledgeQuery query() {
        return KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of("colombia")));
    }

    private KnowledgeCandidate candidate(String content) {
        return new KnowledgeCandidate(content, "src", "Fuente",
            "https://example.com/" + content, PUBLISHED, "application/json", java.util.Map.of());
    }

    @Test
    void fetch_deberiaConcatenarCandidatosDeCadaFuenteEnOrden() {
        var composite = new CompositeKnowledgeSource(List.of(
            query -> List.of(candidate("A."), candidate("B.")),
            query -> List.of(candidate("C."))));

        var candidates = composite.fetch(query());

        assertEquals(List.of("A.", "B.", "C."), candidates.stream().map(KnowledgeCandidate::content).toList());
    }

    @Test
    void fetch_deberiaIgnorarFuentesNulas() {
        var composite = new CompositeKnowledgeSource(java.util.Arrays.asList(
            query -> List.of(candidate("A.")), null));

        var candidates = composite.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("A.", candidates.get(0).content());
    }

    @Test
    void fetch_deberiaIgnorarResultadosNulosDeLasFuentes() {
        var composite = new CompositeKnowledgeSource(List.of(
            query -> null,
            query -> List.of(candidate("A."))));

        var candidates = composite.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("A.", candidates.get(0).content());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoNoHayFuentes() {
        var composite = new CompositeKnowledgeSource(List.of());

        assertTrue(composite.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoQueryNula() {
        var composite = new CompositeKnowledgeSource(List.of(query -> List.of(candidate("A."))));

        assertTrue(composite.fetch(null).isEmpty());
    }

    @Test
    void constructor_deberiaSoportarListaNula() {
        var composite = new CompositeKnowledgeSource(null);

        assertTrue(composite.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaSerDeterminista() {
        var composite = new CompositeKnowledgeSource(List.of(
            query -> List.of(candidate("A.")),
            query -> List.of(candidate("B."))));

        assertEquals(composite.fetch(query()), composite.fetch(query()));
    }

    @Test
    void fetch_deberiaPropagarLaMismaQueryATodasLasFuentes() {
        var seen = new java.util.ArrayList<String>();
        var composite = new CompositeKnowledgeSource(List.of(
            q -> {
                seen.add(q.topic());
                return List.of(candidate("A."));
            },
            q -> {
                seen.add(q.topic());
                return List.of(candidate("B."));
            }));

        composite.fetch(query());

        assertEquals(List.of("retail", "retail"), seen);
    }

    @Test
    void fetch_deberiaExponerCandidatosInmutables() {
        var composite = new CompositeKnowledgeSource(List.of(query -> List.of(candidate("A."))));

        List<KnowledgeCandidate> candidates = composite.fetch(query());

        assertThrows(UnsupportedOperationException.class, () -> candidates.add(null));
    }

    @Test
    void fetch_deberiaTolerarFuenteQueLanzaExcepcion() {
        KnowledgeSource queLanza = query -> {
            throw new IllegalStateException("fuente caída");
        };
        var composite = new CompositeKnowledgeSource(List.of(queLanza));

        assertThrows(IllegalStateException.class, () -> composite.fetch(query()));
    }
}
