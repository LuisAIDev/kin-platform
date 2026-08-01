package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportMetadataFormatterTest {

    private final ReportMetadataFormatter formatter = new ReportMetadataFormatter();

    @Test
    void kind_deberiaSerMetadata() {
        assertEquals(ReportSectionKind.METADATA, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosDatos() {
        var generatedAt = OffsetDateTime.of(2026, 7, 31, 10, 30, 0, 0, ZoneOffset.ofHours(-3));
        var section = new ReportMetadata("2.0.0", "1.0.0", generatedAt, "ReportEngine",
            Map.of("scoring", "1.0", "risk", "1.1"), 85.0, 0.9,
            List.of("ExecutiveSummary", "Scores"));

        var result = formatter.format(section);

        assertTrue(result.contains("## Metadata del Reporte"));
        assertTrue(result.contains("**Versión del reporte:** 2.0.0"));
        assertTrue(result.contains("**Versión de arquitectura:** 1.0.0"));
        assertTrue(result.contains("**Generado el:** 2026-07-31 10:30:00 -03:00"));
        assertTrue(result.contains("**Generado por:** ReportEngine"));
        assertTrue(result.contains("### Versiones de Motores"));
        assertTrue(result.contains("- **scoring:** 1.0"));
        assertTrue(result.contains("- **risk:** 1.1"));
        assertTrue(result.contains("**Cobertura:** 85.0%"));
        assertTrue(result.contains("**Confianza:** 90.0%"));
        assertTrue(result.contains("### Secciones Incluidas (2)"));
        assertTrue(result.contains("- ExecutiveSummary"));
        assertTrue(result.contains("- Scores"));
    }

    @Test
    void format_deberiaOmitirCamposVaciosYMotoresAusentes() {
        var section = new ReportMetadata("", "", OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
            "", Map.of(), 0.0, 0.0, List.of());

        var result = formatter.format(section);

        assertTrue(result.contains("## Metadata del Reporte"));
        assertTrue(result.contains("**Generado el:** 2026-01-01 00:00:00 Z"));
        assertTrue(result.contains("**Cobertura:** 0.0%"));
        assertFalse(result.contains("**Versión del reporte:**"));
        assertFalse(result.contains("**Versión de arquitectura:**"));
        assertFalse(result.contains("**Generado por:**"));
        assertFalse(result.contains("### Versiones de Motores"));
        assertFalse(result.contains("### Secciones Incluidas"));
    }
}
