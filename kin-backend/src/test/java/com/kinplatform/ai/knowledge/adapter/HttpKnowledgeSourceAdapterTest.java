package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpKnowledgeSourceAdapterTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private final RecordingClient client = new RecordingClient(
        new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"));
    private final HttpKnowledgeSourceAdapter adapter = new HttpKnowledgeSourceAdapter(
        "src-http", "API Oficial", "https://example.com/search",
        client, response -> List.of(new HttpKnowledgeSourceAdapter.HttpItem(
            "Dato de mercado.", "https://example.com/report", PUBLISHED)));

    private KnowledgeQuery query() {
        return KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of("colombia")));
    }

    @Test
    void fetch_deberiaConstruirCandidatosConMetaDeEstadoYContenido() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertEquals(1, candidates.size());
        var candidate = candidates.get(0);
        assertEquals("Dato de mercado.", candidate.content());
        assertEquals("src-http", candidate.sourceId());
        assertEquals("API Oficial", candidate.sourceName());
        assertEquals("https://example.com/report", candidate.url());
        assertEquals(PUBLISHED, candidate.publishedAt());
        assertEquals("application/json", candidate.contentType());
        assertEquals("200", candidate.meta().get(SourceValidator.META_HTTP_STATUS));
    }

    @Test
    void fetch_deberiaConstruirRequestConTemaComoParametro() {
        adapter.fetch(query());

        assertEquals("https://example.com/search?q=retail", client.lastRequest.url());
        assertEquals("application/json", client.lastRequest.headers().get("Accept"));
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoQueryNula() {
        assertTrue(adapter.fetch(null).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElClienteFalla() {
        var failing = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com",
            request -> {
                throw new IllegalStateException("red caída");
            }, response -> List.of());

        assertTrue(failing.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElClienteEsNulo() {
        var nulo = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com",
            null, response -> List.of());

        assertTrue(nulo.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoLaRespuestaEsNula() {
        var sinRespuesta = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com",
            request -> null, response -> List.of());

        assertTrue(sinRespuesta.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoNoHayDecoder() {
        var sinDecoder = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com",
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"), null);

        assertTrue(sinDecoder.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoElDecoderDevuelveNulo() {
        var decoderNulo = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com",
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"),
            response -> null);

        assertTrue(decoderNulo.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaOmitirItemsNulos() {
        var conNulos = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com",
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"),
            response -> java.util.Arrays.asList(null, new HttpKnowledgeSourceAdapter.HttpItem(
                "Dato.", "https://example.com/a", PUBLISHED), null));

        var candidates = conNulos.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("Dato.", candidates.get(0).content());
    }

    @Test
    void fetch_deberiaSerDeterminista() {
        assertEquals(adapter.fetch(query()), adapter.fetch(query()));
    }

    @Test
    void constructor_deberiaSoportarNulos() {
        var tolerante = new HttpKnowledgeSourceAdapter(null, null, null,
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(200, null, null),
            response -> List.of(new HttpKnowledgeSourceAdapter.HttpItem(null, null, null)));

        var candidates = tolerante.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("", candidates.get(0).sourceId());
        assertEquals("", candidates.get(0).content());
    }

    @Test
    void httpRequest_deberiaNormalizarNulos() {
        var request = new HttpKnowledgeSourceAdapter.HttpRequest(null, null);

        assertEquals("", request.url());
        assertTrue(request.headers().isEmpty());
    }

    @Test
    void httpResponse_deberiaNormalizarNulos() {
        var response = new HttpKnowledgeSourceAdapter.HttpResponse(500, null, null);

        assertEquals("", response.contentType());
        assertEquals("", response.body());
    }

    @Test
    void httpItem_deberiaNormalizarNulos() {
        var item = new HttpKnowledgeSourceAdapter.HttpItem(null, null, null);

        assertEquals("", item.content());
        assertEquals("", item.url());
        assertNull(item.publishedAt());
    }

    @Test
    void fetch_deberiaUsarSeparadorAmpersand_siLaBaseYaTieneQuery() {
        var recording = new RecordingClient(
            new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"));
        var conQuery = new HttpKnowledgeSourceAdapter("src", "s", "https://example.com/search?lang=es",
            recording, response -> List.of());

        conQuery.fetch(query());

        assertEquals("https://example.com/search?lang=es&q=retail", recording.lastRequest.url());
    }

    @Test
    void fetch_deberiaExponerCandidatosInmutables() {
        List<KnowledgeCandidate> candidates = adapter.fetch(query());

        assertThrows(UnsupportedOperationException.class, () -> candidates.add(null));
    }

    private static class RecordingClient implements HttpKnowledgeSourceAdapter.HttpClient {
        private final HttpKnowledgeSourceAdapter.HttpResponse response;
        private HttpKnowledgeSourceAdapter.HttpRequest lastRequest;

        private RecordingClient(HttpKnowledgeSourceAdapter.HttpResponse response) {
            this.response = response;
        }

        @Override
        public HttpKnowledgeSourceAdapter.HttpResponse fetch(HttpKnowledgeSourceAdapter.HttpRequest request) {
            this.lastRequest = request;
            return response;
        }
    }
}
