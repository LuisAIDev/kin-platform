package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InnovationSectionTest {

    @Test
    void seccion_deberiaProtegerListas() {
        var signals = new ArrayList<>(List.of("automatizacion"));
        var coverage = new ArrayList<>(List.of(new DimensionCoverage(AnalyzedDimension.SOLUTION, true)));
        var section = new InnovationSection("solver", "valor", "mvp", signals, coverage);
        signals.clear();
        coverage.clear();
        assertEquals(1, section.innovationSignals().size());
        assertEquals(1, section.coverage().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.innovationSignals().add("otra"));
    }

    @Test
    void seccion_deberiaAceptarNulos() {
        var section = new InnovationSection(null, null, null, null, null);
        assertEquals("", section.solution());
        assertEquals("", section.valueProposition());
        assertEquals("", section.mvp());
        assertTrue(section.innovationSignals().isEmpty());
        assertTrue(section.coverage().isEmpty());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("Innovation", InnovationSection.empty().sectionName());
        assertEquals(ReportSectionKind.PROJECTION, InnovationSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(InnovationSection.empty().isEmpty());
        var section = new InnovationSection("solver", "", "", List.of(), List.of());
        assertFalse(section.isEmpty());
    }
}
