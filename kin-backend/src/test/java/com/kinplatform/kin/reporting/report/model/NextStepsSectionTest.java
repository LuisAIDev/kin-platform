package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NextStepsSectionTest {

    @Test
    void seccion_deberiaProtegerLista() {
        var steps = new ArrayList<>(List.of(NextStep.of(NextStep.SOURCE_RECOMMENDATION, "A", 7, "razon")));
        var section = new NextStepsSection(steps);
        steps.clear();
        assertEquals(1, section.nextSteps().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.nextSteps().add(NextStep.of(NextStep.SOURCE_OPPORTUNITY, "B", 5, "r")));
    }

    @Test
    void seccion_deberiaAceptarListaNula() {
        var section = new NextStepsSection(null);
        assertTrue(section.nextSteps().isEmpty());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("NextSteps", NextStepsSection.empty().sectionName());
        assertEquals(ReportSectionKind.AGGREGATE, NextStepsSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(NextStepsSection.empty().isEmpty());
        var section = new NextStepsSection(List.of(NextStep.of(NextStep.SOURCE_RECOMMENDATION, "A", 7, "r")));
        assertFalse(section.isEmpty());
    }
}
