package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagKnowledgeSourceTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private final RecordingIndex index = new RecordingIndex(List.of(
        new RagKnowledgeSource.Hit("Fragmento vectorial.", "https://example.com/frag", PUBLISHED, 0.98)));
    private final RagKnowledgeSource adapter = new RagKnowledgeSource("src-rag", "Índice Vectorial", index);

    private KnowledgeQuery query() {
        return KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of("colombia")));
    }

    @Test
    void fetch_deberiaConstruirCandidatosDesdeHits() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertEquals(1, candidates.size());
        var candidate = candidates.get(0);
        assertEquals("Fragmento vectorial.", candidate.content());
        assertEquals("src-rag", candidate.sourceId());
        assertEquals("Índice Vectorial", candidate.sourceName());
        assertEquals("https://example.com/frag", candidate.url());
        assertEquals(PUBLISHED, candidate.publishedAt());
        assertEquals("application/json", candidate.contentType());
        assertEquals("0.98", candidate.meta().get("similarity"));
    }

    @Test
    void fetch_deberiaPropagarTemaYLimiteAlIndice() {
        adapter.fetch(query());

        assertEquals("retail", index.lastText);
        assertEquals(KnowledgeRequest.DEFAULT_LIMIT, index.lastLimit);
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoQueryNula() {
        assertTrue(adapter.fetch(null).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElIndiceFalla() {
        var failing = new RagKnowledgeSource("src", "s", (text, limit) -> {
            throw new IllegalStateException("índice caído");
        });

        assertTrue(failing.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElIndiceEsNulo() {
        var nulo = new RagKnowledgeSource("src", "s", null);

        assertTrue(nulo.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElIndiceDevuelveNulo() {
        var nulo = new RagKnowledgeSource("src", "s", (text, limit) -> null);

        assertTrue(nulo.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaOmitirHitsNulos() {
        var conNulos = new RagKnowledgeSource("src", "s",
            (text, limit) -> java.util.Arrays.asList(null, new RagKnowledgeSource.Hit("Dato.", "https://example.com/a", PUBLISHED, 0.5)));

        var candidates = conNulos.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("Dato.", candidates.get(0).content());
    }

    @Test
    void fetch_deberiaPreservarElOrdenDelIndice() {
        var orden = new RagKnowledgeSource("src", "s",
            (text, limit) -> List.of(
                new RagKnowledgeSource.Hit("A.", "https://example.com/a", PUBLISHED, 0.3),
                new RagKnowledgeSource.Hit("B.", "https://example.com/b", PUBLISHED, 0.9)));

        var candidates = orden.fetch(query());

        assertEquals(List.of("A.", "B."), candidates.stream().map(KnowledgeCandidate::content).toList());
    }

    @Test
    void fetch_deberiaSerDeterminista() {
        assertEquals(adapter.fetch(query()), adapter.fetch(query()));
    }

    @Test
    void constructor_deberiaSoportarNulos() {
        var tolerante = new RagKnowledgeSource(null, null,
            (text, limit) -> List.of(new RagKnowledgeSource.Hit(null, null, null, 0.0)));

        var candidates = tolerante.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("", candidates.get(0).sourceId());
        assertEquals("", candidates.get(0).content());
    }

    @Test
    void hit_deberiaNormalizarNulos() {
        var hit = new RagKnowledgeSource.Hit(null, null, null, 0.0);

        assertEquals("", hit.content());
        assertEquals("", hit.url());
        assertNull(hit.publishedAt());
    }

    @Test
    void fetch_deberiaExponerCandidatosInmutables() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertThrows(UnsupportedOperationException.class, () -> candidates.add(null));
    }

    private static class RecordingIndex implements RagKnowledgeSource.VectorIndex {
        private final List<RagKnowledgeSource.Hit> hits;
        private String lastText;
        private int lastLimit;

        private RecordingIndex(List<RagKnowledgeSource.Hit> hits) {
            this.hits = hits;
        }

        @Override
        public List<RagKnowledgeSource.Hit> search(String text, int limit) {
            this.lastText = text;
            this.lastLimit = limit;
            return hits;
        }
    }
}
