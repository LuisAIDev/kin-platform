package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.ReportModel;
import com.kinplatform.kin.reporting.report.TestReportInputs;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportMetadataAssemblerTest {

    private final ReportMetadataAssembler assembler =
        new ReportMetadataAssembler(ReportModel.defaultModel());

    @Test
    void metadata_deberiaProyectarVersiones() {
        var metadata = assembler.assemble(TestReportInputs.input());
        assertEquals("v1", metadata.reportVersion());
        assertEquals("2.0.0-alpha.1", metadata.architectureVersion());
        assertEquals(ReportMetadataAssembler.GENERATOR_NAME, metadata.generatedBy());
    }

    @Test
    void metadata_deberiaProyectarMapaDeVersionesDeEngines() {
        var metadata = assembler.assemble(TestReportInputs.input());
        assertEquals(Map.of(
            "ScoringEngine", "v1",
            "RecommendationEngine", "v1",
            "RiskEngine", "v1",
            "OpportunityEngine", "v1",
            ReportMetadataAssembler.GENERATOR_NAME, "v1"),
            metadata.engineVersions());
    }

    @Test
    void metadata_deberiaProyectarCoberturaYConfianza() {
        var metadata = assembler.assemble(TestReportInputs.input());
        assertEquals(60.0, metadata.coveragePercent());
        assertEquals(0.75, metadata.confidence());
    }

    @Test
    void metadata_deberiaDejarGeneratedAtYSectionsParaElBuilder() {
        var metadata = assembler.assemble(TestReportInputs.input());
        assertNotNull(metadata.generatedAt());
        assertTrue(metadata.sectionsIncluded().isEmpty());
    }
}
