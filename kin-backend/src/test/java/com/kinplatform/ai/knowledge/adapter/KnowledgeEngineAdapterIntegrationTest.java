package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeEngineAdapterIntegrationTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, java.time.ZoneOffset.ofHours(-5));

    private static final OffsetDateTime STALE = OffsetDateTime.now().minusDays(400);

    private SourceValidator validator() {
        return new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json", "application/pdf"));
    }

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("mercado retail", List.of("retail", "colombia"));
    }

    private KnowledgeSource httpSource(String sourceId, String baseUrl, String itemUrl,
                                       int status, String contentType, String content,
                                       OffsetDateTime publishedAt) {
        return new HttpKnowledgeSourceAdapter(sourceId, "API", baseUrl,
            req -> new HttpKnowledgeSourceAdapter.HttpResponse(status, contentType, "{}"),
            resp -> List.of(new HttpKnowledgeSourceAdapter.HttpItem(content, itemUrl, publishedAt)));
    }

    private KnowledgeSource jdbcSource(String sourceId, String url, String content) {
        return new JdbcKnowledgeSource(sourceId, "Base", "SELECT * FROM knowledge",
            (sql, params) -> List.of(new JdbcKnowledgeSource.Row(content, url, PUBLISHED)));
    }

    private KnowledgeSource ragSource(String sourceId, String url, String content) {
        return new RagKnowledgeSource(sourceId, "RAG",
            (text, limit) -> List.of(new RagKnowledgeSource.Hit(content, url, PUBLISHED, 0.95)));
    }

    private KnowledgeSource documentSource(String sourceId, String url, String content) {
        return new DocumentKnowledgeSource(sourceId, "Docs",
            path -> List.of(new DocumentKnowledgeSource.Document(content, "título", url, PUBLISHED)),
            List.of("docs/informe.pdf"));
    }

    @Test
    void integracion_completa_gatewayUsaCompositeDeAdaptadores() {
        var composite = new CompositeKnowledgeSource(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                200, "application/json", "El mercado retail colombiano creció un 12%.", PUBLISHED),
            jdbcSource("src-jdbc", "https://example.com/estudio", "Las PYMEs digitalizan sus ventas."),
            ragSource("src-rag", "https://example.com/fragmento", "La demanda de logística urbana se triplica."),
            documentSource("src-doc", "https://example.com/informe", "El informe describe el comercio minorista.")
        ));
        var engine = new KnowledgeEngine(new KnowledgeGateway(new SourceRegistry(List.of(composite)), validator()));

        KnowledgeResult result = engine.evaluate(KnowledgeInput.of(request()));

        assertFalse(result.isEmpty());
        assertEquals(4, result.factCount());
        assertEquals(List.of("src-http", "src-jdbc", "src-rag", "src-doc"), result.sourcesUsed());
        assertEquals(List.of("src-http", "src-jdbc", "src-rag", "src-doc"),
            result.facts().stream().map(KnowledgeFact::sourceId).toList());
        assertTrue(result.explanation().contains("4 de 4"));
    }

    @Test
    void integracion_completa_gatewayUsaAdaptadoresIndividuales() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                200, "application/json", "Dato de mercado verificado.", PUBLISHED),
            ragSource("src-rag", "https://example.com/fragmento", "Fragmento del índice vectorial.")
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertEquals(2, result.factCount());
        assertEquals(List.of("src-http", "src-rag"),
            result.facts().stream().map(KnowledgeFact::sourceId).toList());
    }

    @Test
    void adaptadoresVacios_deberianProducirResultadoVacioConMotivo() {
        var composite = new CompositeKnowledgeSource(List.of(
            new HttpKnowledgeSourceAdapter("src-http", "API", "https://example.com/api",
                req -> new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"),
                resp -> List.of()),
            new JdbcKnowledgeSource("src-jdbc", "Base", "SELECT", (sql, params) -> List.of())
        ));

        KnowledgeResult result = new KnowledgeGateway(new SourceRegistry(List.of(composite)), validator())
            .acquire(request());

        assertTrue(result.isEmpty());
        assertEquals(0, result.factCount());
        assertTrue(result.sourcesUsed().isEmpty());
        assertEquals(0.0, result.confidence(), 1e-9);
        assertTrue(result.explanation().contains("No se obtuvieron candidatos"));
    }

    @Test
    void adaptadoresConResultadosNulos_deberianDegradarAGraciosamente() {
        var source = new KnowledgeSource() {
            @Override
            public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
                return null;
            }
        };

        KnowledgeResult result = new KnowledgeGateway(new SourceRegistry(List.of(source)), validator())
            .acquire(request());

        assertTrue(result.isEmpty());
        assertEquals(0, result.validations().size());
        assertTrue(result.explanation().contains("No se obtuvieron candidatos"));
    }

    @Test
    void resultadosDuplicados_deberianDescartarLaSegundaOcurrencia() {
        var composite = new CompositeKnowledgeSource(List.of(
            httpSource("src-http", "https://example.com/a", "https://example.com/report",
                200, "application/json", "Mismo dato verificado.", PUBLISHED),
            httpSource("src-http", "https://example.com/b", "https://example.com/report",
                200, "application/json", "Mismo dato verificado.", PUBLISHED)
        ));

        KnowledgeResult result = new KnowledgeGateway(new SourceRegistry(List.of(composite)), validator())
            .acquire(request());

        assertEquals(1, result.factCount());
        assertEquals(List.of("src-http"), result.sourcesUsed());
        assertEquals(2, result.validations().size());
        assertTrue(result.validations().get(0).accepted());
        assertFalse(result.validations().get(1).accepted());
        assertTrue(result.validations().get(1).reasons().contains("Fuente duplicada (sourceId, url)"));
    }

    @Test
    void resultadosInvalidos_estadoHttpNoExitoso_deberianDescartarse() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                500, "application/json", "Página no disponible.", PUBLISHED)
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertTrue(result.isEmpty());
        assertEquals(1, result.validations().size());
        assertFalse(result.validations().get(0).accepted());
        assertTrue(result.validations().get(0).reasons().contains("Estado HTTP no es 2xx"));
    }

    @Test
    void resultadosInvalidos_protocoloInseguro_deberianDescartarse() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "http://example.com/api", "http://example.com/report",
                200, "application/json", "Dato sin cifrar.", PUBLISHED)
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(result.validations().get(0).reasons().contains("Protocolo HTTPS obligatorio"));
    }

    @Test
    void resultadosInvalidos_tipoDeContenidoNoPermitido_deberianDescartarse() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                200, "text/html", "Página web genérica.", PUBLISHED)
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(result.validations().get(0).reasons().contains("Tipo de contenido no permitido"));
    }

    @Test
    void resultadosInvalidos_contenidoVacio_deberianDescartarse() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                200, "application/json", "", PUBLISHED)
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(result.validations().get(0).reasons().contains("Formato inválido: contenido vacío"));
    }

    @Test
    void resultadosInvalidos_urlMalformada_deberianDescartarse() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/api", "://url-malformada",
                200, "application/json", "Dato con URL inválida.", PUBLISHED)
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(result.validations().get(0).reasons().contains("URL inválida"));
    }

    @Test
    void resultadosInvalidos_fueraDeFrescura_deberianDescartarse() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                200, "application/json", "Dato obsoleto.", STALE)
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertTrue(result.isEmpty());
        assertTrue(result.validations().get(0).reasons().contains("Fuera de la ventana de frescura"));
    }

    @Test
    void propagacionDeErrores_deberiaPropagarseDesdeLaFuenteAlGateway() {
        KnowledgeSource queLanza = query -> {
            throw new IllegalStateException("fuente caída");
        };

        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(queLanza)), validator());

        assertThrows(IllegalStateException.class, () -> gateway.acquire(request()));
    }

    @Test
    void propagacionDeErrores_deberiaPropagarseDesdeLaFuenteDelComposite() {
        KnowledgeSource queLanza = query -> {
            throw new IllegalStateException("índice caído");
        };
        var composite = new CompositeKnowledgeSource(List.of(
            jdbcSource("src-jdbc", "https://example.com/estudio", "Dato previo."),
            queLanza
        ));

        assertThrows(IllegalStateException.class, () -> composite.fetch(KnowledgeQuery.from(request())));
        assertThrows(IllegalStateException.class,
            () -> new KnowledgeGateway(new SourceRegistry(List.of(composite)), validator()).acquire(request()));
    }

    @Test
    void ordenDeterminista_conMultiplesAdaptadores() {
        var gateway = new KnowledgeGateway(new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/a", "https://example.com/report",
                200, "application/json", "Dato A.", PUBLISHED),
            jdbcSource("src-jdbc", "https://example.com/estudio", "Dato B."),
            ragSource("src-rag", "https://example.com/fragmento", "Dato C.")
        )), validator());

        KnowledgeResult r1 = gateway.acquire(request());
        KnowledgeResult r2 = gateway.acquire(request());

        assertEquals(r1, r2);
        assertEquals(List.of("src-http", "src-jdbc", "src-rag"),
            r1.facts().stream().map(KnowledgeFact::sourceId).toList());
        assertEquals(r1.sourcesUsed(), r2.sourcesUsed());
        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
    }

    @Test
    void seleccionCorrecta_medianteSourceRegistry_conFuentesMixtas() {
        var registry = new SourceRegistry(List.of(
            httpSource("src-http", "https://example.com/a", "https://example.com/report",
                200, "application/json", "Dato válido A.", PUBLISHED),
            httpSource("src-err", "https://example.com/b", "https://example.com/err",
                500, "application/json", "Dato inválido B.", PUBLISHED),
            jdbcSource("src-jdbc", "https://example.com/estudio", "Dato válido C.")
        ));

        KnowledgeResult result = new KnowledgeGateway(registry, validator()).acquire(request());

        assertEquals(2, result.factCount());
        assertEquals(List.of("src-http", "src-jdbc"),
            result.facts().stream().map(KnowledgeFact::sourceId).toList());
        assertEquals(3, result.validations().size());
        assertTrue(result.validations().get(1).reasons().contains("Estado HTTP no es 2xx"));
    }

    @Test
    void seleccionCorrecta_medianteSourceRegistry_conRegistroPosterior() {
        var registry = new SourceRegistry(List.of());
        var gateway = new KnowledgeGateway(registry, validator());

        assertTrue(gateway.acquire(request()).isEmpty());

        registry.register(jdbcSource("src-jdbc", "https://example.com/estudio", "Dato registrado después."));
        KnowledgeResult result = gateway.acquire(request());

        assertEquals(1, result.factCount());
        assertEquals(List.of("src-jdbc"), result.sourcesUsed());
    }

    @Test
    void integracion_conKnowledgeEngine_deberiaProducirResultadoCanonizado() {
        var composite = new CompositeKnowledgeSource(List.of(
            httpSource("src-http", "https://example.com/api", "https://example.com/report",
                200, "application/json", "Dato de mercado verificado.", PUBLISHED),
            jdbcSource("src-jdbc", "https://example.com/estudio", "Dato de la base de conocimiento.")
        ));
        var engine = new KnowledgeEngine(new KnowledgeGateway(new SourceRegistry(List.of(composite)), validator()));

        KnowledgeResult result = engine.evaluate(KnowledgeInput.of(request()));

        assertFalse(result.isEmpty());
        assertEquals(2, result.factCount());
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(KnowledgeEngine.ENGINE_VERSION, result.engineVersion());
        assertEquals(EnginePhase.KNOWLEDGE, engine.metadata().phase());
    }
}
