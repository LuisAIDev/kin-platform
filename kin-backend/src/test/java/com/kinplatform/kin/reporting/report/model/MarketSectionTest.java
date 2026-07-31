package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketSectionTest {

    @Test
    void seccion_deberiaProtegerCobertura() {
        var coverage = new ArrayList<>(List.of(new DimensionCoverage(AnalyzedDimension.SECTOR, true)));
        var section = new MarketSection("tech", "pymes", "Buenos Aires", "falta X", coverage);
        coverage.clear();
        assertEquals(1, section.coverage().size());
        assertThrows(UnsupportedOperationException.class,
            () -> section.coverage().add(new DimensionCoverage(AnalyzedDimension.CITY, false)));
    }

    @Test
    void seccion_deberiaAceptarNulos() {
        var section = new MarketSection(null, null, null, null, null);
        assertEquals("", section.sector());
        assertEquals("", section.targetCustomer());
        assertEquals("", section.city());
        assertEquals("", section.problem());
        assertTrue(section.coverage().isEmpty());
    }

    @Test
    void seccion_deberiaExponerNombreYKind() {
        assertEquals("Market", MarketSection.empty().sectionName());
        assertEquals(ReportSectionKind.PROJECTION, MarketSection.empty().kind());
    }

    @Test
    void seccion_vacio_deberiaEstarVacio() {
        assertTrue(MarketSection.empty().isEmpty());
        var section = new MarketSection("tech", "", "", "", List.of());
        assertFalse(section.isEmpty());
    }
}
