package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarketSectionAssemblerTest {

    private final MarketSectionAssembler assembler = new MarketSectionAssembler();

    @Test
    void seccion_deberiaProyectarValoresDeMercado() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals("tech", section.sector());
        assertEquals("pymes", section.targetCustomer());
        assertEquals("Buenos Aires", section.city());
        assertEquals("falta software", section.problem());
    }

    @Test
    void seccion_deberiaProyectarCoberturaDeDimensiones() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(4, section.coverage().size());
        assertTrue(section.coverage().stream().allMatch(dc -> dc.covered()));
    }
}
