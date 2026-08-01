package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcKnowledgeSourceTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private final RecordingDb db = new RecordingDb(List.of(
        new JdbcKnowledgeSource.Row("Dato de mercado.", "https://example.com/report", PUBLISHED)));
    private final JdbcKnowledgeSource adapter =
        new JdbcKnowledgeSource("src-jdbc", "Base Conocimiento", "SELECT * FROM knowledge", db);

    private KnowledgeQuery query() {
        return KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of("colombia")));
    }

    @Test
    void fetch_deberiaConstruirCandidatosDesdeFilas() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertEquals(1, candidates.size());
        var candidate = candidates.get(0);
        assertEquals("Dato de mercado.", candidate.content());
        assertEquals("src-jdbc", candidate.sourceId());
        assertEquals("Base Conocimiento", candidate.sourceName());
        assertEquals("https://example.com/report", candidate.url());
        assertEquals(PUBLISHED, candidate.publishedAt());
        assertEquals("application/json", candidate.contentType());
    }

    @Test
    void fetch_deberiaPropagarLaConsultaALaBase() {
        adapter.fetch(query());

        assertEquals("SELECT * FROM knowledge", db.lastQueryText);
        assertEquals(List.of("retail", "colombia"), db.lastParams);
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoQueryNula() {
        assertTrue(adapter.fetch(null).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoLaBaseFalla() {
        var failing = new JdbcKnowledgeSource("src", "s", "q",
            (sql, params) -> {
                throw new IllegalStateException("bd caída");
            });

        assertTrue(failing.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoLaBaseEsNula() {
        var nula = new JdbcKnowledgeSource("src", "s", "q", null);

        assertTrue(nula.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoLaBaseDevuelveNulo() {
        var nula = new JdbcKnowledgeSource("src", "s", "q", (sql, params) -> null);

        assertTrue(nula.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaOmitirFilasNulas() {
        var conNulas = new JdbcKnowledgeSource("src", "s", "q",
            (sql, params) -> java.util.Arrays.asList(null, new JdbcKnowledgeSource.Row("Dato.", "https://example.com/a", PUBLISHED)));

        var candidates = conNulas.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("Dato.", candidates.get(0).content());
    }

    @Test
    void fetch_deberiaSerDeterminista() {
        assertEquals(adapter.fetch(query()), adapter.fetch(query()));
    }

    @Test
    void constructor_deberiaSoportarNulos() {
        var tolerante = new JdbcKnowledgeSource(null, null, null,
            (sql, params) -> List.of(new JdbcKnowledgeSource.Row(null, null, null)));

        var candidates = tolerante.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("", candidates.get(0).sourceId());
        assertEquals("", candidates.get(0).content());
    }

    @Test
    void row_deberiaNormalizarNulos() {
        var row = new JdbcKnowledgeSource.Row(null, null, null);

        assertEquals("", row.content());
        assertEquals("", row.url());
        assertNull(row.publishedAt());
    }

    @Test
    void fetch_deberiaExponerCandidatosInmutables() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertThrows(UnsupportedOperationException.class, () -> candidates.add(null));
    }

    private static class RecordingDb implements JdbcKnowledgeSource.KnowledgeDb {
        private final List<JdbcKnowledgeSource.Row> rows;
        private String lastQueryText;
        private List<String> lastParams;

        private RecordingDb(List<JdbcKnowledgeSource.Row> rows) {
            this.rows = rows;
        }

        @Override
        public List<JdbcKnowledgeSource.Row> query(String queryText, List<String> params) {
            this.lastQueryText = queryText;
            this.lastParams = new ArrayList<>(params);
            return rows;
        }
    }
}
