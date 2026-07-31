package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinancialSectionAssemblerTest {

    private final FinancialSectionAssembler assembler = new FinancialSectionAssembler();

    @Test
    void seccion_deberiaProyectarValoresFinancieros() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals("suscripcion", section.revenueModel());
        assertEquals("equipo", section.resources());
        assertEquals("crecer", section.objectives());
    }

    @Test
    void seccion_deberiaProyectarCoberturaDeDimensiones() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(3, section.coverage().size());
        assertTrue(section.coverage().stream().allMatch(dc -> dc.covered()));
        assertEquals(com.kinplatform.kin.context.AnalyzedDimension.REVENUE_MODEL,
            section.coverage().get(0).dimension());
    }
}
