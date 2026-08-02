package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SupportVosTest {

    @Test
    void dimensionCoverage_deberiaExponerDimensionYCobertura() {
        var coverage = new DimensionCoverage(AnalyzedDimension.SECTOR, true);
        assertEquals(AnalyzedDimension.SECTOR, coverage.dimension());
        assertTrue(coverage.covered());
    }

    @Test
    void nextStep_deberiaAceptarNulosYAcotarPrioridad() {
        var step = new NextStep(null, null, null, 99, null);
        assertNotNull(step.id());
        assertEquals("", step.source());
        assertEquals("", step.title());
        assertEquals(10, step.priority());
        assertEquals("", step.reason());
    }

    @Test
    void nextStep_of_deberiaGenerarIdDeterminista() {
        var a = NextStep.of(NextStep.SOURCE_RECOMMENDATION, "Titulo", 7, "razon");
        var b = NextStep.of(NextStep.SOURCE_RECOMMENDATION, "Titulo", 7, "razon");
        assertEquals(a.id(), b.id());
        assertEquals(7, a.priority());
    }

    @Test
    void nextStep_deberiaProtegerPrioridadInferior() {
        var step = NextStep.of(NextStep.SOURCE_OPPORTUNITY, "T", -3, "r");
        assertEquals(1, step.priority());
    }

    @Test
    void seccionGenerica_deberiaTenerKindGeneralPorDefecto() {
        ReportSection section = new ReportSection() {
            @Override
            public String sectionName() {
                return "Generica";
            }
        };
        assertEquals(ReportSectionKind.GENERAL, section.kind());
    }

    @Test
    void kind_deberiaContenerTodaLaTaxonomia() {
        assertNotNull(ReportSectionKind.valueOf("GENERAL"));
        assertNotNull(ReportSectionKind.valueOf("EXECUTIVE"));
        assertNotNull(ReportSectionKind.valueOf("SCORING"));
        assertNotNull(ReportSectionKind.valueOf("ANALYTIC"));
        assertNotNull(ReportSectionKind.valueOf("PROJECTION"));
        assertNotNull(ReportSectionKind.valueOf("AGGREGATE"));
        assertNotNull(ReportSectionKind.valueOf("SOURCES"));
        assertNotNull(ReportSectionKind.valueOf("METADATA"));
        assertEquals(8, ReportSectionKind.values().length);
    }
}
