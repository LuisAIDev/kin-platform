package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoresSectionFormatterTest {

    private final ScoresSectionFormatter formatter = new ScoresSectionFormatter();

    @Test
    void kind_deberiaSerScoring() {
        assertEquals(ReportSectionKind.SCORING, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosDatos() {
        var section = new ScoresSection(85, 100, Map.of("Mercado", 80, "Producto", 90),
            "VIABLE", 85.0, List.of("Fortaleza 1"), List.of("Debilidad 1"), "v1.0");

        var result = formatter.format(section);

        assertTrue(result.contains("## Scoring de Viabilidad"));
        assertTrue(result.contains("**Total:** 85 / 100"));
        assertTrue(result.contains("**Viabilidad:** VIABLE"));
        assertTrue(result.contains("**Confianza:** 85.0%"));
        assertTrue(result.contains("**Modelo:** v1.0"));
        assertTrue(result.contains("### Desglose por Categoría"));
        assertTrue(result.contains("- **Mercado:** 80"));
        assertTrue(result.contains("- **Producto:** 90"));
        assertTrue(result.contains("### Fortalezas"));
        assertTrue(result.contains("- Fortaleza 1"));
        assertTrue(result.contains("### Debilidades"));
        assertTrue(result.contains("- Debilidad 1"));
    }

    @Test
    void format_deberiaManejarSeccionSinDesgloseFortalezasDebilidades() {
        var section = new ScoresSection(0, 0, Map.of(), "", 0.0,
            List.of(), List.of(), "");

        var result = formatter.format(section);

        assertTrue(result.contains("## Scoring de Viabilidad"));
        assertTrue(result.contains("**Total:** 0 / 0"));
        assertFalse(result.contains("### Desglose por Categoría"));
        assertFalse(result.contains("### Fortalezas"));
        assertFalse(result.contains("### Debilidades"));
    }
}
