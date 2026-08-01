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

class DocumentKnowledgeSourceTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private final RecordingParser parser = new RecordingParser(List.of(
        new DocumentKnowledgeSource.Document("Informe oficial.", "Informe", "https://example.com/informe", PUBLISHED)));
    private final DocumentKnowledgeSource adapter = new DocumentKnowledgeSource(
        "src-doc", "Documentos", parser, List.of("docs/informe.pdf"));

    private KnowledgeQuery query() {
        return KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of("colombia")));
    }

    @Test
    void fetch_deberiaConstruirCandidatosDesdeDocumentos() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertEquals(1, candidates.size());
        var candidate = candidates.get(0);
        assertEquals("Informe oficial.", candidate.content());
        assertEquals("src-doc", candidate.sourceId());
        assertEquals("Documentos", candidate.sourceName());
        assertEquals("https://example.com/informe", candidate.url());
        assertEquals(PUBLISHED, candidate.publishedAt());
        assertEquals("application/pdf", candidate.contentType());
    }

    @Test
    void fetch_deberiaIngerirLasRutasConfiguradas() {
        adapter.fetch(query());

        assertEquals(List.of("docs/informe.pdf"), parser.parsedPaths);
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoQueryNula() {
        assertTrue(adapter.fetch(null).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElParserFalla() {
        var failing = new DocumentKnowledgeSource("src", "s", path -> {
            throw new IllegalStateException("parser caído");
        }, List.of("a.pdf"));

        assertTrue(failing.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElParserEsNulo() {
        var nulo = new DocumentKnowledgeSource("src", "s", null, List.of("a.pdf"));

        assertTrue(nulo.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoNoHayRutas() {
        var sinRutas = new DocumentKnowledgeSource("src", "s", path -> List.of(
            new DocumentKnowledgeSource.Document("Dato.", "T", "https://example.com/a", PUBLISHED)), List.of());

        assertTrue(sinRutas.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaOmitirDocumentosNulos() {
        var conNulos = new DocumentKnowledgeSource("src", "s", path -> java.util.Arrays.asList(null,
            new DocumentKnowledgeSource.Document("Dato.", "T", "https://example.com/a", PUBLISHED)),
            List.of("a.pdf"));

        var candidates = conNulos.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("Dato.", candidates.get(0).content());
    }

    @Test
    void fetch_deberiaOmitirParseNulo() {
        var conParseNulo = new DocumentKnowledgeSource("src", "s", path -> null, List.of("a.pdf"));

        assertTrue(conParseNulo.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaConcatenarDocumentosDeVariasRutasEnOrden() {
        var multiRuta = new DocumentKnowledgeSource("src", "s", path -> {
            if (path.endsWith("a.pdf")) {
                return List.of(new DocumentKnowledgeSource.Document("A.", "T", "https://example.com/a", PUBLISHED));
            }
            return List.of(new DocumentKnowledgeSource.Document("B.", "T", "https://example.com/b", PUBLISHED));
        }, List.of("a.pdf", "b.pdf"));

        var candidates = multiRuta.fetch(query());

        assertEquals(List.of("A.", "B."), candidates.stream().map(KnowledgeCandidate::content).toList());
    }

    @Test
    void fetch_deberiaSerDeterminista() {
        assertEquals(adapter.fetch(query()), adapter.fetch(query()));
    }

    @Test
    void constructor_deberiaSoportarNulos() {
        var tolerante = new DocumentKnowledgeSource(null, null, path -> List.of(
            new DocumentKnowledgeSource.Document(null, null, null, null)), null);

        var candidates = tolerante.fetch(query());

        assertTrue(candidates.isEmpty());
    }

    @Test
    void document_deberiaNormalizarNulos() {
        var document = new DocumentKnowledgeSource.Document(null, null, null, null);

        assertEquals("", document.content());
        assertEquals("", document.title());
        assertEquals("", document.url());
        assertNull(document.publishedAt());
    }

    @Test
    void fetch_deberiaExponerCandidatosInmutables() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertThrows(UnsupportedOperationException.class, () -> candidates.add(null));
    }

    private static class RecordingParser implements DocumentKnowledgeSource.DocumentParser {
        private final List<DocumentKnowledgeSource.Document> documents;
        private List<String> parsedPaths;

        private RecordingParser(List<DocumentKnowledgeSource.Document> documents) {
            this.documents = documents;
        }

        @Override
        public List<DocumentKnowledgeSource.Document> parse(String path) {
            if (parsedPaths == null) {
                parsedPaths = new java.util.ArrayList<>();
            }
            parsedPaths.add(path);
            return documents;
        }
    }
}
