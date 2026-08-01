package com.kinplatform.kin.knowledge;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeQueryTest {

    @Test
    void from_deberiaProyectarElRequest() {
        var request = KnowledgeRequest.of("mercado", List.of("retail", "colombia"));
        var query = KnowledgeQuery.from(request);

        assertEquals("mercado", query.topic());
        assertEquals(List.of("retail", "colombia"), query.keywords());
        assertEquals(request.limit(), query.limit());
        assertEquals(request.timeWindow(), query.timeWindow());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var query = new KnowledgeQuery(null, null, 0, null);

        assertEquals("", query.topic());
        assertTrue(query.keywords().isEmpty());
        assertEquals(1, query.limit());
        assertEquals(KnowledgeRequest.DEFAULT_TIME_WINDOW, query.timeWindow());
    }

    @Test
    void constructor_deberiaAcotarElLimite() {
        assertEquals(KnowledgeRequest.MAX_LIMIT, new KnowledgeQuery("t", List.of(), 999, null).limit());
        assertEquals(1, new KnowledgeQuery("t", List.of(), 0, null).limit());
    }

    @Test
    void constructor_deberiaConservarValoresValidos() {
        var query = new KnowledgeQuery("tema", List.of("a"), 4, Duration.ofDays(10));
        assertEquals("tema", query.topic());
        assertEquals(List.of("a"), query.keywords());
        assertEquals(4, query.limit());
        assertEquals(Duration.ofDays(10), query.timeWindow());
    }

    @Test
    void constructor_deberiaProtegerLaLista() {
        var keywords = new ArrayList<>(List.of("retail"));
        var query = new KnowledgeQuery("mercado", keywords, 5, null);

        keywords.add("otro");
        assertThrows(UnsupportedOperationException.class,
            () -> query.keywords().add("extra"));
        assertEquals(1, query.keywords().size());
    }
}
