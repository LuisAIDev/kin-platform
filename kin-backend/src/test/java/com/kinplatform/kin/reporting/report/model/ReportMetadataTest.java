package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportMetadataTest {

    @Test
    void metadata_deberiaProtegerMapaYLista() {
        var versions = new HashMap<String, String>(Map.of("ScoringEngine", "v1"));
        var sections = new ArrayList<>(List.of("Scores"));
        var metadata = new ReportMetadata("v1", "2.0.0-alpha.1", null, "ReportEngine",
            versions, 60.0, 0.7, sections);

        versions.put("RiskEngine", "v1");
        sections.add("Risks");

        assertEquals(1, metadata.engineVersions().size());
        assertEquals(1, metadata.sectionsIncluded().size());
        assertThrows(UnsupportedOperationException.class,
            () -> metadata.engineVersions().put("x", "v1"));
        assertThrows(UnsupportedOperationException.class,
            () -> metadata.sectionsIncluded().add("y"));
    }

    @Test
    void metadata_deberiaAceptarNulosYAcotar() {
        var metadata = new ReportMetadata(null, null, null, null, null, -5.0, 2.0, null);
        assertEquals("", metadata.reportVersion());
        assertEquals("", metadata.architectureVersion());
        assertNotNull(metadata.generatedAt());
        assertEquals("", metadata.generatedBy());
        assertTrue(metadata.engineVersions().isEmpty());
        assertEquals(0.0, metadata.coveragePercent());
        assertEquals(1.0, metadata.confidence());
        assertTrue(metadata.sectionsIncluded().isEmpty());
    }

    @Test
    void metadata_deberiaAcotarCoberturaSuperior() {
        var metadata = new ReportMetadata("v1", "a", null, "g", Map.of(), 120.0, 0.5, List.of());
        assertEquals(100.0, metadata.coveragePercent());
    }

    @Test
    void withGeneratedAt_deberiaReemplazarTimestamp() {
        var metadata = new ReportMetadata("v1", "a", null, "g", Map.of(), 0.0, 0.0, List.of());
        var fixed = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        assertEquals(fixed, metadata.withGeneratedAt(fixed).generatedAt());
    }

    @Test
    void withSectionsIncluded_deberiaCopiarLista() {
        var metadata = new ReportMetadata("v1", "a", null, "g", Map.of(), 0.0, 0.0, List.of());
        var sections = new ArrayList<>(List.of("Scores", "Risks"));
        var result = metadata.withSectionsIncluded(sections);
        sections.clear();
        assertEquals(2, result.sectionsIncluded().size());
        assertEquals(List.of("Scores", "Risks"), result.sectionsIncluded());
    }

    @Test
    void metadata_deberiaExponerNombreYKind() {
        assertEquals("ReportMetadata", ReportMetadata.empty().sectionName());
        assertEquals(ReportSectionKind.METADATA, ReportMetadata.empty().kind());
    }
}
