package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinancialSectionFormatterTest {

    private final FinancialSectionFormatter formatter = new FinancialSectionFormatter();

    @Test
    void kind_deberiaSerProjection() {
        assertEquals(ReportSectionKind.PROJECTION, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosDatosConCobertura() {
        var coverage = List.of(
            new DimensionCoverage(AnalyzedDimension.REVENUE_MODEL, true),
            new DimensionCoverage(AnalyzedDimension.RESOURCES, false)
        );
        var section = new FinancialSection("Suscripción", "Servidor x4", "Objetivo A", coverage);

        var result = formatter.format(section);

        assertTrue(result.contains("## Proyección Financiera"));
        assertTrue(result.contains("**Modelo de ingresos:** Suscripción"));
        assertTrue(result.contains("**Recursos:** Servidor x4"));
        assertTrue(result.contains("**Objetivos financieros:** Objetivo A"));
        assertTrue(result.contains("### Cobertura de Dimensión"));
        assertTrue(result.contains("- **Modelo de ingresos:** Cubierto"));
        assertTrue(result.contains("- **Recursos necesarios:** No cubierto"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoSeccionVacia() {
        var result = formatter.format(FinancialSection.empty());

        assertTrue(result.contains("## Proyección Financiera"));
        assertTrue(result.contains("_Sin datos financieros disponibles._"));
        assertFalse(result.contains("**Modelo de ingresos:**"));
    }
}
