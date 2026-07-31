package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.RecommendationCategory;
import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationsSectionAssemblerTest {

    private final RecommendationsSectionAssembler assembler = new RecommendationsSectionAssembler();

    @Test
    void seccion_deberiaProyectarResultado() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(2, section.recommendations().size());
        assertEquals("Recomendaci\u00F3n A", section.recommendations().get(0).title());
        assertEquals(9, section.priority());
        assertEquals(0.8, section.confidence());
        assertEquals(RecommendationCategory.STRATEGY, section.dominantCategory());
        assertEquals(List.of("Recomendaci\u00F3n A", "Recomendaci\u00F3n B"),
            section.recommendations().stream().map(r -> r.title()).toList());
    }
}
