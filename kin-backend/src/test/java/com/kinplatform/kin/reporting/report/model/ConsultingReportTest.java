package com.kinplatform.kin.reporting.report.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultingReportTest {

    private static final UUID ZERO = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private ConsultingReport populated() {
        return new ConsultingReport(
            ZERO, ZERO,
            new ExecutiveSummary("Proyecto", "Cat", 80, 100, "VIABLE", 60.0, "texto", java.util.List.of("H")),
            new ScoresSection(80, 100, java.util.Map.of("Mercado", 80), "VIABLE", 80.0,
                java.util.List.of(), java.util.List.of(), "v1"),
            new RecommendationsSection(java.util.List.of(), 0, 0.0,
                com.kinplatform.kin.reporting.RecommendationCategory.VALIDATION),
            new RisksSection(java.util.List.of(), com.kinplatform.kin.reporting.risk.RiskLevel.LOW,
                java.util.List.of(), 0.0),
            new OpportunitiesSection(java.util.List.of(), java.util.List.of(), 0.0),
            FinancialSection.empty(), MarketSection.empty(), InnovationSection.empty(),
            NextStepsSection.empty(),
            new ReportMetadata("v1", "2.0.0-alpha.1", null, "ReportEngine",
                java.util.Map.of("ScoringEngine", "v1"), 60.0, 0.7, java.util.List.of("Scores"))
        );
    }

    @Test
    void reporte_deberiaMapearContratoEngineResult() {
        var report = populated();
        assertEquals(0.7, report.confidence());
        assertEquals("texto", report.explanation());
        assertEquals("ReportEngine", report.generatedBy());
        assertEquals("v1", report.engineVersion());
    }

    @Test
    void reporte_vacio_deberiaEstarVacio() {
        var report = ConsultingReport.empty();
        assertTrue(report.isEmpty());
        assertFalse(populated().isEmpty());
    }

    @Test
    void reporte_vacio_deberiaSerFallbackSeguro() {
        var report = ConsultingReport.empty();
        assertEquals(ZERO, report.id());
        assertEquals(ZERO, report.projectId());
        assertEquals(0.0, report.confidence());
        assertEquals("", report.explanation());
    }

    @Test
    void reporte_deberiaAceptarSeccionesNulas() {
        var report = new ConsultingReport(ZERO, ZERO, null, null, null, null, null, null, null, null, null, null);
        assertNotNull(report.executiveSummary());
        assertNotNull(report.scores());
        assertNotNull(report.recommendations());
        assertNotNull(report.risks());
        assertNotNull(report.opportunities());
        assertNotNull(report.financial());
        assertNotNull(report.market());
        assertNotNull(report.innovation());
        assertNotNull(report.nextSteps());
        assertNotNull(report.metadata());
    }

    @Test
    void reporte_deberiaAceptarIdsNulos() {
        var report = new ConsultingReport(null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(ZERO, report.id());
        assertEquals(ZERO, report.projectId());
    }
}
