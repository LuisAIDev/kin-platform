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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiConnectorTest {

    private static final OffsetDateTime PUBLISHED =
        OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private KnowledgeQuery query() {
        return KnowledgeQuery.from(KnowledgeRequest.of("retail", List.of("colombia")));
    }

    private KnowledgeCandidate candidate(String content) {
        return new KnowledgeCandidate(content, "src", "Fuente",
            "https://example.com/" + content, PUBLISHED, "application/json", Map.of());
    }

    @Test
    void name_deberiaSerConfigurable() {
        assertEquals("PublicApiConnector", new PublicApiConnector(List.of()).name());
        assertEquals("Conector DANE", new PublicApiConnector("Conector DANE", List.of()).name());
    }

    @Test
    void fetch_deberiaAgregarCandidatosDeCadaFuenteOficialEnOrden() {
        var connector = new PublicApiConnector(List.of(
            query -> List.of(candidate("A.")),
            query -> List.of(candidate("B."), candidate("C."))));

        var candidates = connector.fetch(query());

        assertEquals(List.of("A.", "B.", "C."), candidates.stream().map(KnowledgeCandidate::content).toList());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoNoHayFuentes() {
        var connector = new PublicApiConnector(List.of());

        assertTrue(connector.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaRetornarVacio_cuandoQueryNula() {
        var connector = new PublicApiConnector(List.of(query -> List.of(candidate("A."))));

        assertTrue(connector.fetch(null).isEmpty());
    }

    @Test
    void fetch_deberiaIgnorarFuentesNulasYResultadosNulos() {
        var connector = new PublicApiConnector(java.util.Arrays.asList(
            query -> null,
            null,
            query -> List.of(candidate("A."))));

        var candidates = connector.fetch(query());

        assertEquals(1, candidates.size());
        assertEquals("A.", candidates.get(0).content());
    }

    @Test
    void constructor_deberiaSoportarListaNula() {
        var connector = new PublicApiConnector((List<KnowledgeSource>) null);

        assertTrue(connector.fetch(query()).isEmpty());
    }

    @Test
    void fetch_deberiaSerDeterminista() {
        var connector = new PublicApiConnector(List.of(query -> List.of(candidate("A."))));

        assertEquals(connector.fetch(query()), connector.fetch(query()));
    }

    @Test
    void name_deberiaNormalizarNulos() {
        assertEquals("", new PublicApiConnector((String) null, List.of()).name());
    }
}
