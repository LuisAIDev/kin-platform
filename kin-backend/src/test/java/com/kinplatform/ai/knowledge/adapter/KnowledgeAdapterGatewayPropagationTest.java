package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeAdapterGatewayPropagationTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private KnowledgeSource httpSource() {
        return new HttpKnowledgeSourceAdapter("src-http", "API Oficial", "https://example.com/search",
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"),
            response -> List.of(new HttpKnowledgeSourceAdapter.HttpItem(
                "El mercado retail colombiano creció un 12% anual.",
                "https://example.com/report", PUBLISHED)));
    }

    private KnowledgeSource jdbcSource() {
        return new JdbcKnowledgeSource("src-jdbc", "Base Conocimiento", "SELECT * FROM knowledge",
            (sql, params) -> List.of(new JdbcKnowledgeSource.Row(
                "Las PYMEs digitalizan sus ventas en 2026.",
                "https://example.com/estudio", PUBLISHED)));
    }

    private KnowledgeSource ragSource() {
        return new RagKnowledgeSource("src-rag", "Índice Vectorial",
            (text, limit) -> List.of(new RagKnowledgeSource.Hit(
                "La demanda de logística urbana se triplica.",
                "https://example.com/fragmento", PUBLISHED, 0.97)));
    }

    private KnowledgeSource documentSource() {
        return new DocumentKnowledgeSource("src-doc", "Documentos",
            path -> List.of(new DocumentKnowledgeSource.Document(
                "El informe sectorial describe el comercio minorista.",
                "Informe", "https://example.com/informe", PUBLISHED)),
            List.of("docs/informe.pdf"));
    }

    private SourceValidator validator() {
        return new SourceValidator(Set.of("example.com"), Duration.ofDays(365),
            Set.of("application/json", "application/pdf"));
    }

    private KnowledgeRequest request() {
        return KnowledgeRequest.of("mercado retail", List.of("retail", "colombia"));
    }

    @Test
    void gateway_deberiaRecibirCandidatosDeLosAdaptadoresCompuestos() {
        var composite = new CompositeKnowledgeSource(List.of(httpSource(), jdbcSource()));
        var connector = new PublicApiConnector(List.of(ragSource(), documentSource()));
        var registry = new SourceRegistry(List.of(composite, connector));
        var engine = new KnowledgeEngine(new KnowledgeGateway(registry, validator()));

        KnowledgeResult result = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));

        assertFalse(result.isEmpty());
        assertEquals(4, result.factCount());
        assertEquals(4, result.sourcesUsed().size());
        assertTrue(result.facts().stream().allMatch(f -> f.trust().name() != null));
        assertTrue(result.explanation().contains("4 de 4"));
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.generatedBy());
    }

    @Test
    void gateway_deberiaDescartarCandidatosQueNoPasanLaValidacion() {
        var noPermitido = new HttpKnowledgeSourceAdapter("src-http", "API", "https://otro.com/search",
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(200, "application/json", "{}"),
            response -> List.of(new HttpKnowledgeSourceAdapter.HttpItem(
                "Dato de dominio no permitido.", "https://otro.com/report", PUBLISHED)));
        var registry = new SourceRegistry(List.of(noPermitido));
        var engine = new KnowledgeEngine(new KnowledgeGateway(registry, validator()));

        KnowledgeResult result = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));

        assertTrue(result.isEmpty());
        assertEquals(0, result.factCount());
        assertEquals(1, result.validations().size());
        assertFalse(result.validations().get(0).accepted());
    }

    @Test
    void gateway_deberiaPropagarElMetaHttpStatusHaciaLaValidacion() {
        var statusError = new HttpKnowledgeSourceAdapter("src-http", "API", "https://example.com/search",
            request -> new HttpKnowledgeSourceAdapter.HttpResponse(404, "application/json", "{}"),
            response -> List.of(new HttpKnowledgeSourceAdapter.HttpItem(
                "Página no encontrada.", "https://example.com/report", PUBLISHED)));
        var registry = new SourceRegistry(List.of(statusError));
        var engine = new KnowledgeEngine(new KnowledgeGateway(registry, validator()));

        KnowledgeResult result = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));

        assertTrue(result.isEmpty());
        assertEquals(1, result.validations().size());
        assertTrue(result.validations().get(0).reasons().contains("Estado HTTP no es 2xx"));
    }

    @Test
    void gateway_deberiaDegradarAGraciosamente_sinFuentes() {
        var engine = new KnowledgeEngine(new KnowledgeGateway(SourceRegistry.empty(), validator()));

        KnowledgeResult result = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));

        assertTrue(result.isEmpty());
        assertEquals(0.0, result.confidence(), 1e-9);
    }

    @Test
    void gateway_deberiaSerDeterminista_conAdaptadores() {
        var registry = new SourceRegistry(List.of(httpSource(), jdbcSource()));
        var engine = new KnowledgeEngine(new KnowledgeGateway(registry, validator()));

        KnowledgeResult r1 = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));
        KnowledgeResult r2 = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));

        assertEquals(r1.facts(), r2.facts());
        assertEquals(r1.confidence(), r2.confidence(), 1e-9);
        assertEquals(r1.sourcesUsed(), r2.sourcesUsed());
    }

    @Test
    void gateway_deberiaRecibirCandidatosDeAdaptadoresIndividuales() {
        var registry = new SourceRegistry(List.of(ragSource()));
        var engine = new KnowledgeEngine(new KnowledgeGateway(registry, validator()));

        KnowledgeResult result = engine.evaluate(com.kinplatform.kin.knowledge.KnowledgeInput.of(request()));

        assertFalse(result.isEmpty());
        assertEquals(1, result.factCount());
        assertEquals(List.of("src-rag"), result.sourcesUsed());
        assertTrue(result.facts().get(0).claim().contains("logística urbana"));
    }
}
