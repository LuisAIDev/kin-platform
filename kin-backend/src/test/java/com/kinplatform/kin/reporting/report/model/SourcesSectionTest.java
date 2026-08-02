package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.enrichment.EvidenceCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourcesSectionTest {

    @Test
    void sectionName_deberiaSerSources() {
        assertEquals("Sources", SourcesSection.empty().sectionName());
    }

    @Test
    void kind_deberiaSerSources() {
        assertEquals(ReportSectionKind.SOURCES, SourcesSection.empty().kind());
    }

    @Test
    void vacia_deberiaEstarVacia() {
        var section = SourcesSection.empty();
        assertTrue(section.isEmpty());
        assertTrue(section.sources().isEmpty());
    }

    @Test
    void conFuentes_deberiaEstarNoVacia() {
        var source = new CitedSource("s1", "https://x", "claim", EvidenceCategory.MARKET, 0.8);
        var section = new SourcesSection(List.of(source));
        assertFalse(section.isEmpty());
        assertEquals(1, section.sources().size());
        assertEquals(source, section.sources().get(0));
    }

    @Test
    void constructor_deberiaNormalizarNulos() {
        var section = new SourcesSection(null);
        assertNotNull(section.sources());
        assertTrue(section.sources().isEmpty());
    }
}
