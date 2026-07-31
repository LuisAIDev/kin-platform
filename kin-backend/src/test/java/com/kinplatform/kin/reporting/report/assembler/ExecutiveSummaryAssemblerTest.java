package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutiveSummaryAssemblerTest {

    private final ExecutiveSummaryAssembler assembler = new ExecutiveSummaryAssembler();

    @Test
    void resumen_deberiaProyectarIdentidadYScore() {
        var summary = assembler.assemble(TestReportInputs.input());
        assertEquals("Proyecto", summary.projectName());
        assertEquals("tech", summary.projectCategory());
        assertEquals(78, summary.overallScore());
        assertEquals(100, summary.maxScore());
        assertEquals("VIABLE", summary.viabilityLabel());
        assertEquals(60.0, summary.coveragePercent());
    }

    @Test
    void resumen_deberiaReafirmarScoreYCoberturaEnElTexto() {
        var summary = assembler.assemble(TestReportInputs.input());
        assertTrue(summary.summaryText().contains("cobertura del 60%"));
        assertTrue(summary.summaryText().contains("78/100"));
        assertTrue(summary.summaryText().contains("VIABLE"));
        assertTrue(summary.summaryText().contains("2 recomendaciones"));
        assertTrue(summary.summaryText().contains("1 riesgos"));
        assertTrue(summary.summaryText().contains("2 oportunidades"));
    }

    @Test
    void resumen_deberiaAgregarHighlightsDeFortalezasYOportunidades() {
        var summary = assembler.assemble(TestReportInputs.input());
        assertTrue(summary.keyHighlights().contains("fortaleza 1"));
        assertTrue(summary.keyHighlights().contains("Oportunidad A"));
        assertFalse(summary.keyHighlights().contains("Oportunidad B"));
    }
}
