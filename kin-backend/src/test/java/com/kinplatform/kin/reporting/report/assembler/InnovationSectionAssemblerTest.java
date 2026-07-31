package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InnovationSectionAssemblerTest {

    private final InnovationSectionAssembler assembler = new InnovationSectionAssembler();

    @Test
    void seccion_deberiaProyectarValoresDeInnovacion() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals("app", section.solution());
        assertEquals("ahorro", section.valueProposition());
        assertEquals("piloto", section.mvp());
    }

    @Test
    void seccion_deberiaFiltrarSoloSenalesDeInnovacion() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(List.of("innovaci\u00F3n en distribuci\u00F3n"), section.innovationSignals());
    }

    @Test
    void seccion_deberiaProyectarCoberturaDeDimensiones() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(3, section.coverage().size());
        assertTrue(section.coverage().stream().allMatch(dc -> dc.covered()));
    }
}
