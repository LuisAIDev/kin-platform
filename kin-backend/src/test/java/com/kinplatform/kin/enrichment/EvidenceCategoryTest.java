package com.kinplatform.kin.enrichment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvidenceCategoryTest {

    @Test
    void deberiaExponerLasCuatroCategoriasObjetivo() {
        assertEquals(4, EvidenceCategory.values().length);
        assertEquals(EvidenceCategory.MARKET, EvidenceCategory.valueOf("MARKET"));
        assertEquals(EvidenceCategory.INNOVATION, EvidenceCategory.valueOf("INNOVATION"));
        assertEquals(EvidenceCategory.FINANCIAL, EvidenceCategory.valueOf("FINANCIAL"));
        assertEquals(EvidenceCategory.COMPETITIVE, EvidenceCategory.valueOf("COMPETITIVE"));
    }

    @Test
    void deberiaExponerNombresDeVisualizacion() {
        assertEquals("Mercado", EvidenceCategory.MARKET.displayName());
        assertEquals("Innovación", EvidenceCategory.INNOVATION.displayName());
        assertEquals("Financiero", EvidenceCategory.FINANCIAL.displayName());
        assertEquals("Competitivo", EvidenceCategory.COMPETITIVE.displayName());
    }
}
