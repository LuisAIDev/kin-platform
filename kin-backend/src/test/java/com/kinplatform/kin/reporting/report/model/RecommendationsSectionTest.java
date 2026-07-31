package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationCategory;
import com.kinplatform.kin.reporting.RecommendationExplanation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationsSectionTest {

    private Recommendation rec(String title, int priority) {
        return Recommendation.create(RecommendationCategory.VALIDATION, title, "desc",
            priority, ImpactLevel.HIGH, EffortLevel.LOW, AnalyzedDimension.PROBLEM,
            List.of("paso"), "resultado", RecommendationExplanation.of(List.of(), "r", "y"));
    }

    @Test
    void seccion_deberiaProtegerLista() {
        var list = new ArrayList<>(List.of(rec("A", 7)));
        var section = new RecommendationsSection(list, 7, 0.8,
            RecommendationCategory.VALIDATION);
        list.clear();
        assertEquals(1, section.recommendations().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.recommendations().add(rec("B", 5)));
    }

    @Test
    void seccion_deberiaAceptarNulos() {
        var section = new RecommendationsSection(null, 99, -1.0, null);
        assertTrue(section.recommendations().isEmpty());
        assertEquals(10, section.priority());
        assertEquals(0.0, section.confidence());
        assertEquals(RecommendationCategory.VALIDATION, section.dominantCategory());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("Recommendations", RecommendationsSection.empty().sectionName());
        assertEquals(ReportSectionKind.ANALYTIC, RecommendationsSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(RecommendationsSection.empty().isEmpty());
        var section = new RecommendationsSection(List.of(rec("A", 7)), 7, 0.8,
            RecommendationCategory.VALIDATION);
        assertFalse(section.isEmpty());
    }
}
