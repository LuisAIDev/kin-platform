package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.ReportModel;
import com.kinplatform.kin.reporting.report.TestReportInputs;
import com.kinplatform.kin.reporting.report.model.NextStep;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import com.kinplatform.kin.reporting.risk.RiskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NextStepsSectionAssemblerTest {

    private final NextStepsSectionAssembler assembler =
        new NextStepsSectionAssembler(ReportModel.defaultModel());

    @Test
    void seccion_deberiaAgregarTopPorFuenteEnOrden() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals(5, section.nextSteps().size());
        assertEquals(NextStep.SOURCE_RECOMMENDATION, section.nextSteps().get(0).source());
        assertEquals("Recomendaci\u00F3n A", section.nextSteps().get(0).title());
        assertEquals(NextStep.SOURCE_RECOMMENDATION, section.nextSteps().get(1).source());
        assertEquals(NextStep.SOURCE_RISK_MITIGATION, section.nextSteps().get(2).source());
        assertEquals(NextStep.SOURCE_OPPORTUNITY, section.nextSteps().get(3).source());
    }

    @Test
    void seccion_deberiaOrdenarPorPrioridadDescendenteYLimitar() {
        var recs = List.of(
            TestReportInputs.recommendation("R1", 2),
            TestReportInputs.recommendation("R2", 5),
            TestReportInputs.recommendation("R3", 9),
            TestReportInputs.recommendation("R4", 1));
        var risks = List.of(
            TestReportInputs.risk("K1", RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.MEDIUM),
            TestReportInputs.risk("K2", RiskLevel.CRITICAL, RiskLevel.CRITICAL, RiskLevel.CRITICAL),
            TestReportInputs.risk("K3", RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.MEDIUM));
        var opps = List.of(
            TestReportInputs.opportunity("O1", 3),
            TestReportInputs.opportunity("O2", 7),
            TestReportInputs.opportunity("O3", 6));

        var input = new ReportInput(TestReportInputs.PROJECT_ID, "t", "c",
            TestReportInputs.context(), TestReportInputs.evaluation(),
            TestReportInputs.input().decision(), TestReportInputs.score(),
            new RecommendationResult(recs, 9, 0.8, null, "e", "g", "v1"),
            new RiskResult(risks, RiskLevel.HIGH, List.of(), 0.7, "e", "g", "v1"),
            new OpportunityResult(opps, List.of(), 0.8, "e", "g", "v1"));

        var section = assembler.assemble(input);
        assertEquals(5, section.nextSteps().size());
        assertEquals(9, section.nextSteps().get(0).priority());
        assertEquals("R3", section.nextSteps().get(0).title());
        assertEquals(5, section.nextSteps().get(1).priority());
        assertEquals(2, section.nextSteps().get(2).priority());
        assertEquals(NextStep.SOURCE_RISK_MITIGATION, section.nextSteps().get(3).source());
        assertEquals("K2", section.nextSteps().get(3).title());
        assertEquals("K3", section.nextSteps().get(4).title());
    }

    @Test
    void seccion_conResultadosVacios_deberiaEstarVacia() {
        var empty = new ReportInput(TestReportInputs.PROJECT_ID, "t", "c",
            TestReportInputs.context(), TestReportInputs.evaluation(),
            TestReportInputs.input().decision(), TestReportInputs.score(),
            RecommendationResult.empty(), RiskResult.empty(), OpportunityResult.empty());
        var section = assembler.assemble(empty);
        assertTrue(section.nextSteps().isEmpty());
    }

    @Test
    void seccion_deberiaUsarReglasComoRazon() {
        var section = assembler.assemble(TestReportInputs.input());
        assertEquals("regla-recomendacion", section.nextSteps().get(0).reason());
    }
}
