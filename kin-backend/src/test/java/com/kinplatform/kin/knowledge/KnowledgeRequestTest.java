package com.kinplatform.kin.knowledge;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeRequestTest {

    @Test
    void of_deberiaConstruirConValoresPorDefecto() {
        var request = KnowledgeRequest.of("mercado", List.of("retail", "colombia"));

        assertEquals("mercado", request.topic());
        assertEquals(List.of("retail", "colombia"), request.keywords());
        assertTrue(request.dimensions().isEmpty());
        assertEquals(KnowledgeRequest.DEFAULT_LIMIT, request.limit());
        assertEquals(KnowledgeRequest.DEFAULT_TIME_WINDOW, request.timeWindow());
    }

    @Test
    void empty_deberiaEstarVacia() {
        var request = KnowledgeRequest.empty();

        assertEquals("", request.topic());
        assertTrue(request.dimensions().isEmpty());
        assertTrue(request.keywords().isEmpty());
        assertEquals(KnowledgeRequest.DEFAULT_LIMIT, request.limit());
        assertEquals(KnowledgeRequest.DEFAULT_TIME_WINDOW, request.timeWindow());
    }

    @Test
    void constructor_deberiaExponerCampos() {
        var dimensions = Set.of(AnalyzedDimension.SECTOR);
        var request = new KnowledgeRequest("financiero", dimensions, List.of("fondo"), 7, Duration.ofDays(30));

        assertEquals("financiero", request.topic());
        assertEquals(dimensions, request.dimensions());
        assertEquals(List.of("fondo"), request.keywords());
        assertEquals(7, request.limit());
        assertEquals(Duration.ofDays(30), request.timeWindow());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var request = new KnowledgeRequest(null, null, null, 0, null);

        assertEquals("", request.topic());
        assertTrue(request.dimensions().isEmpty());
        assertTrue(request.keywords().isEmpty());
        assertEquals(1, request.limit());
        assertEquals(KnowledgeRequest.DEFAULT_TIME_WINDOW, request.timeWindow());
    }

    @Test
    void constructor_deberiaAcotarElLimite() {
        assertEquals(1, new KnowledgeRequest("t", Set.of(), List.of(), 0, null).limit());
        assertEquals(KnowledgeRequest.MAX_LIMIT,
            new KnowledgeRequest("t", Set.of(), List.of(), 999, null).limit());
        assertEquals(1,
            new KnowledgeRequest("t", Set.of(), List.of(), -5, null).limit());
    }

    @Test
    void constructor_deberiaProtegerListas() {
        var keywords = new ArrayList<>(List.of("retail"));
        var dimensions = new LinkedHashSet<>(Set.of(AnalyzedDimension.SECTOR));
        var request = new KnowledgeRequest("mercado", dimensions, keywords, 5, null);

        keywords.add("otro");
        dimensions.add(AnalyzedDimension.CITY);
        assertThrows(UnsupportedOperationException.class,
            () -> request.keywords().add("extra"));
        assertThrows(UnsupportedOperationException.class,
            () -> request.dimensions().add(AnalyzedDimension.PROBLEM));
        assertEquals(1, request.keywords().size());
        assertEquals(1, request.dimensions().size());
    }
}
