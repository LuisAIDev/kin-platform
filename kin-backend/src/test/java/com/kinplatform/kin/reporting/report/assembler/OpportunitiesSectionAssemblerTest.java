package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpportunitiesSectionAssemblerTest {

    private final OpportunitiesSectionAssembler assembler = new OpportunitiesSectionAssembler();

    @Test
    void seccion_deberiaProyectarResultado() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(2, section.opportunities().size());
        assertEquals("Oportunidad A", section.opportunities().get(0).title());
        assertEquals(1, section.topOpportunities().size());
        assertEquals(0.8, section.confidence());
    }
}
