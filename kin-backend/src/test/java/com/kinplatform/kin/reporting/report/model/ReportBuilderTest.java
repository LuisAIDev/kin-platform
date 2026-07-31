package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportBuilderTest {

    private static final UUID PROJECT = UUID.randomUUID();

    private ReportMetadata metadata(String version) {
        return new ReportMetadata(version, "2.0.0-alpha.1", null, "ReportEngine",
            Map.of("ScoringEngine", "v1"), 60.0, 0.7, List.of());
    }

    private ReportBuilder fullBuilder(String version) {
        return ReportBuilder.create(PROJECT)
            .executiveSummary(new ExecutiveSummary("Proyecto", "Cat", 80, 100, "VIABLE",
                60.0, "texto", List.of("H")))
            .scores(new ScoresSection(80, 100, Map.of("Mercado", 80), "VIABLE", 80.0,
                List.of(), List.of(), "v1"))
            .recommendations(new RecommendationsSection(List.of(), 0, 0.0,
                com.kinplatform.kin.reporting.RecommendationCategory.VALIDATION))
            .risks(new RisksSection(List.of(), com.kinplatform.kin.reporting.risk.RiskLevel.LOW,
                List.of(), 0.0))
            .opportunities(new OpportunitiesSection(List.of(), List.of(), 0.0))
            .financial(new FinancialSection("modelo", "recursos", "objetivos",
                List.of(new DimensionCoverage(com.kinplatform.kin.context.AnalyzedDimension.REVENUE_MODEL, true))))
            .market(new MarketSection("tech", "pymes", "BA", "problema", List.of()))
            .innovation(new InnovationSection("sol", "valor", "mvp", List.of(), List.of()))
            .nextSteps(new NextStepsSection(List.of(NextStep.of(NextStep.SOURCE_RECOMMENDATION, "A", 7, "razon"))))
            .metadata(metadata(version));
    }

    @Test
    void create_conProjectIdNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> ReportBuilder.create(null));
    }

    @Test
    void setterDuplicado_deberiaLanzar() {
        var builder = fullBuilder("v1");
        assertThrows(IllegalStateException.class,
            () -> builder.scores(new ScoresSection(0, 0, Map.of(), "", 0.0, List.of(), List.of(), "")));
        assertThrows(IllegalStateException.class,
            () -> builder.executiveSummary(ExecutiveSummary.empty()));
        assertThrows(IllegalStateException.class,
            () -> builder.metadata(metadata("v1")));
    }

    @Test
    void validate_deberiaExigirSeccionesObligatorias() {
        assertThrows(IllegalStateException.class,
            () -> ReportBuilder.create(PROJECT)
                .scores(ScoresSection.empty()).metadata(metadata("v1")).validate());
        assertThrows(IllegalStateException.class,
            () -> ReportBuilder.create(PROJECT)
                .executiveSummary(ExecutiveSummary.empty()).metadata(metadata("v1")).validate());
        assertThrows(IllegalStateException.class,
            () -> ReportBuilder.create(PROJECT)
                .executiveSummary(ExecutiveSummary.empty()).scores(ScoresSection.empty()).validate());
        assertThrows(IllegalStateException.class,
            () -> ReportBuilder.create(PROJECT).validate());
    }

    @Test
    void build_deberiaDerivarIdDeterministaPorProyectoYVersion() {
        var report = fullBuilder("v1").build();
        var otro = fullBuilder("v1").build();
        var otraVersion = fullBuilder("v2").build();
        assertEquals(report.id(), otro.id());
        assertNotEquals(report.id(), otraVersion.id());
        assertEquals(PROJECT, report.projectId());
    }

    @Test
    void build_deberiaDerivarSectionsIncludedDeLasSeccionesEnsambladas() {
        var report = fullBuilder("v1").build();
        assertEquals(List.of(
            "ExecutiveSummary", "Scores", "Recommendations", "Risks", "Opportunities",
            "Financial", "Market", "Innovation", "NextSteps", "ReportMetadata"),
            report.metadata().sectionsIncluded());
    }

    @Test
    void build_deberiaComputarGeneratedAtEnMetadata() {
        var before = OffsetDateTime.now().minusSeconds(1);
        var report = fullBuilder("v1").build();
        assertNotNull(report.metadata().generatedAt());
        assertTrue(report.metadata().generatedAt().isAfter(before));
    }

    @Test
    void build_conSeccionesOpcionalesOmitidas_deberiaCompletarConEmpty() {
        var report = ReportBuilder.create(PROJECT)
            .executiveSummary(ExecutiveSummary.empty())
            .scores(ScoresSection.empty())
            .metadata(metadata("v1"))
            .build();
        assertTrue(report.nextSteps().isEmpty());
        assertTrue(report.opportunities().isEmpty());
        assertEquals(List.of("ExecutiveSummary", "Scores", "ReportMetadata"),
            report.metadata().sectionsIncluded());
    }
}
