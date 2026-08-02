package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.enrichment.EvidenceCategory;
import com.kinplatform.kin.reporting.report.model.CitedSource;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import com.kinplatform.kin.reporting.report.model.SourcesSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourcesSectionFormatterTest {

    private final SourcesSectionFormatter formatter = new SourcesSectionFormatter();

    @Test
    void kind_deberiaSerSources() {
        assertEquals(ReportSectionKind.SOURCES, formatter.kind());
    }

    @Test
    void formato_vacio_deberiaIndicarSinFuentes() {
        var output = formatter.format(SourcesSection.empty());
        assertTrue(output.contains("## Fuentes Citadas"));
        assertTrue(output.contains("_Sin fuentes externas citadas._"));
    }

    @Test
    void formato_conFuentes_deberiaIncluirClaimFuenteCategoriaYScore() {
        var section = new SourcesSection(List.of(
            new CitedSource("src-1", "https://x/1", "El mercado crece 15%", EvidenceCategory.MARKET, 0.8),
            new CitedSource("src-2", "", "Margen 20%", EvidenceCategory.FINANCIAL, 0.6)));

        var output = formatter.format(section);

        assertTrue(output.contains("## Fuentes Citadas"));
        assertTrue(output.contains("El mercado crece 15%"));
        assertTrue(output.contains("https://x/1"));
        assertTrue(output.contains("Mercado"));
        assertTrue(output.contains("Margen 20%"));
        assertTrue(output.contains("src-2"));
        assertTrue(output.contains("Financiero"));
        assertTrue(output.contains("0.80"));
        assertTrue(output.contains("0.60"));
        assertTrue(output.contains("2 fuentes citadas"));
    }

    @Test
    void formato_deberiaUsarSourceId_cuandoNoHayUrl() {
        var section = new SourcesSection(List.of(
            new CitedSource("src-9", "", "Claim sin url", EvidenceCategory.COMPETITIVE, 0.5)));

        var output = formatter.format(section);

        assertTrue(output.contains("src-9"));
        assertTrue(output.contains("Competitivo"));
        assertFalse(output.contains("https://"));
    }
}
