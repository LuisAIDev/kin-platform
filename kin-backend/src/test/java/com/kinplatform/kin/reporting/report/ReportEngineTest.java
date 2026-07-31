package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.reporting.report.assembler.ExecutiveSummaryAssembler;
import com.kinplatform.kin.reporting.report.assembler.FinancialSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.InnovationSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.MarketSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.NextStepsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.OpportunitiesSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.RecommendationsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ReportMetadataAssembler;
import com.kinplatform.kin.reporting.report.assembler.RisksSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ScoresSectionAssembler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportEngineTest {

    private ReportEngine engine() {
        var model = ReportModel.defaultModel();
        var assemblers = new ReportAssemblers(
            new ExecutiveSummaryAssembler(),
            new ScoresSectionAssembler(),
            new RecommendationsSectionAssembler(),
            new RisksSectionAssembler(),
            new OpportunitiesSectionAssembler(),
            new FinancialSectionAssembler(),
            new MarketSectionAssembler(),
            new InnovationSectionAssembler(),
            new NextStepsSectionAssembler(model),
            new ReportMetadataAssembler(model));
        return new ReportEngine(assemblers, model);
    }

    @Test
    void evaluate_conEntradaCompleta_deberiaEnsamblarElReporte() {
        var report = engine().evaluate(TestReportInputs.input());
        assertEquals(TestReportInputs.PROJECT_ID, report.projectId());
        assertEquals("Proyecto", report.executiveSummary().projectName());
        assertEquals(78, report.scores().totalScore());
        assertEquals(2, report.recommendations().recommendations().size());
        assertEquals(1, report.risks().risks().size());
        assertEquals(2, report.opportunities().opportunities().size());
        assertEquals(3, report.financial().coverage().size());
        assertEquals("suscripcion", report.financial().revenueModel());
        assertEquals("v1", report.engineVersion());
        assertEquals(10, report.metadata().sectionsIncluded().size());
        assertFalse(report.isEmpty());
    }

    @Test
    void evaluate_conEntradaNula_deberiaDevolverVacio() {
        assertTrue(engine().evaluate(null).isEmpty());
    }

    @Test
    void evaluate_conContextoNulo_deberiaDevolverVacio() {
        var base = TestReportInputs.input();
        var input = new ReportInput(base.projectId(), base.projectTitle(), base.projectCategory(),
            null, base.evaluation(), base.decision(), base.score(),
            base.recommendation(), base.risk(), base.opportunity());
        assertTrue(engine().evaluate(input).isEmpty());
    }

    @Test
    void evaluate_conEvaluacionNula_deberiaDevolverVacio() {
        var base = TestReportInputs.input();
        var input = new ReportInput(base.projectId(), base.projectTitle(), base.projectCategory(),
            base.projectContext(), null, base.decision(), base.score(),
            base.recommendation(), base.risk(), base.opportunity());
        assertTrue(engine().evaluate(input).isEmpty());
    }

    @Test
    void evaluate_conScoreNulo_deberiaDevolverVacio() {
        var base = TestReportInputs.input();
        var input = new ReportInput(base.projectId(), base.projectTitle(), base.projectCategory(),
            base.projectContext(), base.evaluation(), base.decision(), null,
            base.recommendation(), base.risk(), base.opportunity());
        assertTrue(engine().evaluate(input).isEmpty());
    }

    @Test
    void evaluate_deberiaProducirIdDeterminista() {
        var engine = engine();
        var report1 = engine.evaluate(TestReportInputs.input());
        var report2 = engine.evaluate(TestReportInputs.input());
        assertEquals(report1.id(), report2.id());
    }

    @Test
    void metadata_deberiaDeclararFaseReporte() {
        var metadata = engine().metadata();
        assertEquals("ReportEngine", metadata.name());
        assertEquals("v1", metadata.version());
        assertEquals(EnginePhase.REPORTING, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals(70, metadata.priority());
    }

    @Test
    void assemblers_deberiaExponerLaAgrupacionInyectada() {
        assertNotNull(engine().assemblers());
    }

    @Test
    void generatorName_deberiaSerReportEngine() {
        assertEquals("ReportEngine", ReportEngine.GENERATOR_NAME);
        assertEquals(ReportEngine.GENERATOR_NAME, ReportMetadataAssembler.GENERATOR_NAME);
    }
}
