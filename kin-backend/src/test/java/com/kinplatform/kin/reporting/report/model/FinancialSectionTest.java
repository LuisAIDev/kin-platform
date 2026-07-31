package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinancialSectionTest {

    @Test
    void seccion_deberiaProtegerCobertura() {
        var coverage = new ArrayList<>(List.of(new DimensionCoverage(
            com.kinplatform.kin.context.AnalyzedDimension.REVENUE_MODEL, true)));
        var section = new FinancialSection("suscripcion", "recurso", "objetivo", coverage);
        coverage.clear();
        assertEquals(1, section.coverage().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.coverage().add(new DimensionCoverage(
                com.kinplatform.kin.context.AnalyzedDimension.OBJECTIVES, false)));
    }

    @Test
    void seccion_deberiaAceptarNulos() {
        var section = new FinancialSection(null, null, null, null);
        assertEquals("", section.revenueModel());
        assertEquals("", section.resources());
        assertEquals("", section.objectives());
        assertTrue(section.coverage().isEmpty());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("Financial", FinancialSection.empty().sectionName());
        assertEquals(ReportSectionKind.PROJECTION, FinancialSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(FinancialSection.empty().isEmpty());
        var section = new FinancialSection("suscripcion", "", "", List.of());
        assertFalse(section.isEmpty());
    }
}
