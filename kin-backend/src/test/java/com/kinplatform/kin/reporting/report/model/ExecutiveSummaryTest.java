package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutiveSummaryTest {

    @Test
    void summary_deberiaProtegerListaDeHighlights() {
        var highlights = new ArrayList<>(List.of("H1"));
        var summary = new ExecutiveSummary("Proyecto", "Cat", 80, 100, "VIABLE",
            60.0, "texto", highlights);
        highlights.clear();
        assertEquals(1, summary.keyHighlights().size());
        assertThrows(UnsupportedOperationException.class,
            () -> summary.keyHighlights().add("H2"));
    }

    @Test
    void summary_deberiaAceptarNulos() {
        var summary = new ExecutiveSummary(null, null, -5, -10, null, 200.0, null, null);
        assertEquals("", summary.projectName());
        assertEquals("", summary.projectCategory());
        assertEquals(0, summary.overallScore());
        assertEquals(0, summary.maxScore());
        assertEquals("", summary.viabilityLabel());
        assertEquals(100.0, summary.coveragePercent());
        assertEquals("", summary.summaryText());
        assertTrue(summary.keyHighlights().isEmpty());
    }

    @Test
    void summary_deberiaAcotarCobertura() {
        var summary = new ExecutiveSummary("p", "c", 0, 0, "", -1.0, "", List.of());
        assertEquals(0.0, summary.coveragePercent());
    }

    @Test
    void summary_deberiaExponerNombreYKind() {
        var summary = ExecutiveSummary.empty();
        assertEquals("ExecutiveSummary", summary.sectionName());
        assertEquals(ReportSectionKind.EXECUTIVE, summary.kind());
    }

    @Test
    void summary_vacio_deberiaEstarVacio() {
        assertTrue(ExecutiveSummary.empty().isEmpty());
        var summary = new ExecutiveSummary("p", "c", 0, 100, "", 0.0, "", List.of());
        assertFalse(summary.isEmpty());
    }
}
