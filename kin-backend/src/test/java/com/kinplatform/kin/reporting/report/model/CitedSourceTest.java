package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.enrichment.EvidenceCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CitedSourceTest {

    @Test
    void constructor_deberiaNormalizarNulos() {
        var source = new CitedSource(null, null, null, null, -1.0);
        assertEquals("", source.sourceId());
        assertEquals("", source.url());
        assertEquals("", source.claim());
        assertEquals(EvidenceCategory.MARKET, source.category());
        assertEquals(0.0, source.score());
    }

    @Test
    void score_deberiaAcotarseAlRango() {
        assertEquals(1.0, new CitedSource("s", "", "", EvidenceCategory.MARKET, 5.0).score());
        assertEquals(0.0, new CitedSource("s", "", "", EvidenceCategory.MARKET, -1.0).score());
    }

    @Test
    void isEmpty_deberiaDetectarFuenteVacia() {
        assertTrue(new CitedSource("", "", "", EvidenceCategory.MARKET, 0.5).isEmpty());
        assertFalse(new CitedSource("s1", "", "", EvidenceCategory.MARKET, 0.5).isEmpty());
        assertFalse(new CitedSource("", "https://x", "", EvidenceCategory.MARKET, 0.5).isEmpty());
    }
}
