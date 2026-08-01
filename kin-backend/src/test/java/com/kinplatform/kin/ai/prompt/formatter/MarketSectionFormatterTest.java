package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketSectionFormatterTest {

    private final MarketSectionFormatter formatter = new MarketSectionFormatter();

    @Test
    void kind_deberiaSerProjection() {
        assertEquals(ReportSectionKind.PROJECTION, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosDatosConCobertura() {
        var coverage = List.of(
            new DimensionCoverage(AnalyzedDimension.TARGET_CUSTOMER, true),
            new DimensionCoverage(AnalyzedDimension.COMPETITION, false)
        );
        var section = new MarketSection("Gastronomía", "Profesionales de 25-40", "Buenos Aires",
            "No hay oferta saludable cerca", coverage);

        var result = formatter.format(section);

        assertTrue(result.contains("## Análisis de Mercado"));
        assertTrue(result.contains("**Sector:** Gastronomía"));
        assertTrue(result.contains("**Cliente objetivo:** Profesionales de 25-40"));
        assertTrue(result.contains("**Ciudad:** Buenos Aires"));
        assertTrue(result.contains("**Problema a resolver:** No hay oferta saludable cerca"));
        assertTrue(result.contains("### Cobertura de Dimensión"));
        assertTrue(result.contains("- **Cliente objetivo:** Cubierto"));
        assertTrue(result.contains("- **Competencia:** No cubierto"));
    }

    @Test
    void format_noDeberiaIncluirCamposVacios() {
        var section = new MarketSection("Sector X", "", "", "", List.of());

        var result = formatter.format(section);

        assertTrue(result.contains("**Sector:** Sector X"));
        assertFalse(result.contains("**Cliente objetivo:**"));
        assertFalse(result.contains("**Ciudad:**"));
        assertFalse(result.contains("**Problema a resolver:**"));
        assertFalse(result.contains("### Cobertura de Dimensión"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoSeccionVacia() {
        var result = formatter.format(MarketSection.empty());

        assertTrue(result.contains("## Análisis de Mercado"));
        assertTrue(result.contains("_Sin datos de mercado disponibles._"));
    }
}
