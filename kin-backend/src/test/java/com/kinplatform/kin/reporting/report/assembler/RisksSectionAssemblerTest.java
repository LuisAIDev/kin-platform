package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.TestReportInputs;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RisksSectionAssemblerTest {

    private final RisksSectionAssembler assembler = new RisksSectionAssembler();

    @Test
    void seccion_deberiaProyectarResultado() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(1, section.risks().size());
        assertEquals("Riesgo A", section.risks().get(0).title());
        assertEquals(RiskLevel.HIGH, section.overallRiskLevel());
        assertEquals(1, section.topRisks().size());
        assertEquals(0.7, section.confidence());
    }
}
