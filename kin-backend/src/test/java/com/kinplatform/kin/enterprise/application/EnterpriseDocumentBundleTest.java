package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseDocumentBundleTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private Map<DocumentType, Map<RenderFormat, byte[]>> renderings() {
        EnumMap<DocumentType, Map<RenderFormat, byte[]>> outer = new EnumMap<>(DocumentType.class);
        EnumMap<RenderFormat, byte[]> inner = new EnumMap<>(RenderFormat.class);
        inner.put(RenderFormat.PDF, "pdf".getBytes());
        inner.put(RenderFormat.DOCX, "docx".getBytes());
        outer.put(DocumentType.LEAN_CANVAS, inner);
        return outer;
    }

    @Test
    void of_deberiaExponerIdentidadYVersion() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 3, renderings());

        assertEquals(PROJECT_ID, bundle.projectId());
        assertEquals(3, bundle.version());
        assertEquals(1, bundle.documentCount());
        assertEquals(2, bundle.renderingCount());
        assertTrue(bundle.types().contains(DocumentType.LEAN_CANVAS));
    }

    @Test
    void rendering_deberiaDevolverLosBytesDelFormato() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, renderings());

        var pdf = bundle.rendering(DocumentType.LEAN_CANVAS, RenderFormat.PDF);
        assertTrue(pdf.isPresent());
        assertEquals("pdf", new String(pdf.get()));
    }

    @Test
    void rendering_conTipoAusente_deberiaDevolverVacio() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, renderings());

        assertTrue(bundle.rendering(DocumentType.KPI, RenderFormat.PDF).isEmpty());
        assertTrue(bundle.rendering(null, RenderFormat.PDF).isEmpty());
        assertTrue(bundle.rendering(DocumentType.LEAN_CANVAS, RenderFormat.PPTX).isEmpty());
        assertTrue(bundle.rendering(DocumentType.LEAN_CANVAS, null).isEmpty());
    }

    @Test
    void renderingsFor_deberiaDevolverElMapaPorFormato() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, renderings());

        var renderings = bundle.renderingsFor(DocumentType.LEAN_CANVAS);
        assertEquals(2, renderings.size());
        assertEquals("pdf", new String(renderings.get(RenderFormat.PDF)));
    }

    @Test
    void renderingsFor_conTipoAusente_deberiaDevolverMapaVacio() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, renderings());

        assertTrue(bundle.renderingsFor(DocumentType.ROADMAP).isEmpty());
        assertTrue(bundle.renderingsFor(null).isEmpty());
    }

    @Test
    void of_deberiaCopiarLosBytesDefensivamente() {
        var source = renderings();
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, source);

        var pdfBytes = source.get(DocumentType.LEAN_CANVAS).get(RenderFormat.PDF);
        pdfBytes[0] = 'X';

        var rendered = bundle.rendering(DocumentType.LEAN_CANVAS, RenderFormat.PDF).orElseThrow();
        assertEquals('p', rendered[0]);
    }

    @Test
    void rendering_noDeberiaExponerLosBytesInternos() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, renderings());

        var first = bundle.rendering(DocumentType.LEAN_CANVAS, RenderFormat.PDF).orElseThrow();
        first[0] = 'X';

        var second = bundle.rendering(DocumentType.LEAN_CANVAS, RenderFormat.PDF).orElseThrow();
        assertEquals('p', second[0]);
    }

    @Test
    void documents_deberiaSerInmutable() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, renderings());

        assertThrows(UnsupportedOperationException.class,
            () -> bundle.documents().put(DocumentType.KPI, Map.of()));
        assertThrows(UnsupportedOperationException.class,
            () -> bundle.renderingsFor(DocumentType.LEAN_CANVAS).put(RenderFormat.PPTX, new byte[0]));
    }

    @Test
    void of_conProjectIdNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseDocumentBundle.of(null, 1, renderings()));
    }

    @Test
    void of_conMapaNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseDocumentBundle.of(PROJECT_ID, 1, null));
    }

    @Test
    void of_conClaveNula_deberiaLanzar() {
        java.util.Map<DocumentType, java.util.Map<RenderFormat, byte[]>> outer = new java.util.HashMap<>();
        outer.put(null, java.util.Map.of());
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseDocumentBundle.of(PROJECT_ID, 1, outer));
    }

    @Test
    void of_conFormatoNulo_deberiaLanzar() {
        java.util.Map<DocumentType, java.util.Map<RenderFormat, byte[]>> outer = new java.util.HashMap<>();
        java.util.Map<RenderFormat, byte[]> inner = new java.util.HashMap<>();
        inner.put(null, new byte[0]);
        outer.put(DocumentType.KPI, inner);
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseDocumentBundle.of(PROJECT_ID, 1, outer));
    }

    @Test
    void of_conBytesNulos_deberiaLanzar() {
        EnumMap<DocumentType, Map<RenderFormat, byte[]>> outer = new EnumMap<>(DocumentType.class);
        EnumMap<RenderFormat, byte[]> inner = new EnumMap<>(RenderFormat.class);
        inner.put(RenderFormat.PDF, null);
        outer.put(DocumentType.KPI, inner);
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseDocumentBundle.of(PROJECT_ID, 1, outer));
    }

    @Test
    void bundleVacio_deberiaExponerCeros() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, Map.of());

        assertEquals(0, bundle.documentCount());
        assertEquals(0, bundle.renderingCount());
        assertTrue(bundle.types().isEmpty());
        assertFalse(bundle.rendering(DocumentType.KPI, RenderFormat.PDF).isPresent());
    }
}
