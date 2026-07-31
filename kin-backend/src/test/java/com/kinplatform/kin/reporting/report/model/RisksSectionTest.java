package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RisksSectionTest {

    private Risk risk(String title) {
        return Risk.create(RiskCategory.BUSINESS, title, "desc",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.MEDIUM, 0.7,
            RiskExplanation.of(List.of(), "r", "y", "e"), List.of("R1"),
            AnalyzedDimension.RISKS, "v1");
    }

    @Test
    void seccion_deberiaProtegerListas() {
        var risks = new ArrayList<>(List.of(risk("A")));
        var top = new ArrayList<>(List.of(risk("A")));
        var section = new RisksSection(risks, RiskLevel.HIGH, top, 0.7);
        risks.clear();
        top.clear();
        assertEquals(1, section.risks().size());
        assertEquals(1, section.topRisks().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.risks().add(risk("B")));
        assertThrows(UnsupportedOperationException.class,
            () -> section.topRisks().add(risk("B")));
    }

    @Test
    void seccion_deberiaAceptarNulos() {
        var section = new RisksSection(null, null, null, 2.0);
        assertTrue(section.risks().isEmpty());
        assertEquals(RiskLevel.LOW, section.overallRiskLevel());
        assertTrue(section.topRisks().isEmpty());
        assertEquals(1.0, section.confidence());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("Risks", RisksSection.empty().sectionName());
        assertEquals(ReportSectionKind.ANALYTIC, RisksSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(RisksSection.empty().isEmpty());
        var section = new RisksSection(List.of(risk("A")), RiskLevel.HIGH,
            List.of(), 0.7);
        assertFalse(section.isEmpty());
    }
}
