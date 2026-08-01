package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutiveSummaryFormatterTest {

    private final ExecutiveSummaryFormatter formatter = new ExecutiveSummaryFormatter();

    @Test
    void kind_deberiaSerExecutive() {
        assertEquals(ReportSectionKind.EXECUTIVE, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosDatos() {
        var section = new ExecutiveSummary("Mi App", "Software", 85, 100, "VIABLE",
            75.0, "Resumen breve", List.of("Punto 1", "Punto 2"));

        var result = formatter.format(section);

        assertTrue(result.contains("## Resumen Ejecutivo"));
        assertTrue(result.contains("**Proyecto:** Mi App"));
        assertTrue(result.contains("**Categoría:** Software"));
        assertTrue(result.contains("**Score Global:** 85 / 100"));
        assertTrue(result.contains("**Viabilidad:** VIABLE"));
        assertTrue(result.contains("**Cobertura:** 75.0%"));
        assertTrue(result.contains("Resumen breve"));
        assertTrue(result.contains("**Puntos destacados:**"));
        assertTrue(result.contains("- Punto 1"));
        assertTrue(result.contains("- Punto 2"));
    }

    @Test
    void format_deberiaManejarSeccionVacia() {
        var result = formatter.format(ExecutiveSummary.empty());

        assertTrue(result.contains("## Resumen Ejecutivo"));
        assertTrue(result.contains("**Score Global:** 0 / 0"));
        assertFalse(result.contains("**Puntos destacados:**"));
    }

    @Test
    void format_noDeberiaIncluirPuntosDestacadosCuandoEstanVacios() {
        var section = new ExecutiveSummary("App", "Cat", 50, 100, "MEDIA",
            50.0, "", List.of());

        var result = formatter.format(section);

        assertFalse(result.contains("**Puntos destacados:**"));
        assertFalse(result.contains("Resumen breve"));
    }
}
