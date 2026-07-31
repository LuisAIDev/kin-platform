package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityExplanation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpportunitiesSectionTest {

    private Opportunity opp(String title) {
        return Opportunity.create(OpportunityCategory.MERCADO, title, "desc",
            7, ImpactLevel.HIGH, EffortLevel.MEDIUM, 0.8,
            OpportunityExplanation.of(List.of("dato"), "regla", "motivo", "evidencia"),
            List.of("R1"), AnalyzedDimension.PROBLEM, "v1");
    }

    @Test
    void seccion_deberiaProtegerListas() {
        var opportunities = new ArrayList<>(List.of(opp("A")));
        var top = new ArrayList<>(List.of(opp("A")));
        var section = new OpportunitiesSection(opportunities, top, 0.8);
        opportunities.clear();
        top.clear();
        assertEquals(1, section.opportunities().size());
        assertEquals(1, section.topOpportunities().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.opportunities().add(opp("B")));
        assertThrows(UnsupportedOperationException.class,
            () -> section.topOpportunities().add(opp("B")));
    }

    @Test
    void seccion_deberiaAceptarNulos() {
        var section = new OpportunitiesSection(null, null, -1.0);
        assertTrue(section.opportunities().isEmpty());
        assertTrue(section.topOpportunities().isEmpty());
        assertEquals(0.0, section.confidence());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("Opportunities", OpportunitiesSection.empty().sectionName());
        assertEquals(ReportSectionKind.ANALYTIC, OpportunitiesSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(OpportunitiesSection.empty().isEmpty());
        var section = new OpportunitiesSection(List.of(opp("A")), List.of(), 0.8);
        assertFalse(section.isEmpty());
    }
}
