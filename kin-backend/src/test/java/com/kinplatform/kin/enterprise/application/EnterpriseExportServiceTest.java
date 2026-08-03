package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseExportServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private final EnterpriseExportService service = new EnterpriseExportService(new EnterpriseRendererFactory());

    @Test
    void export_deberiaGenerarTodosLosDocumentosEnLosTresFormatos() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1,
            DocumentType.LEAN_CANVAS, DocumentType.FINANCIAL_PLAN, DocumentType.KPI);

        var bundle = service.export(project);

        assertEquals(PROJECT_ID, bundle.projectId());
        assertEquals(1, bundle.version());
        assertEquals(3, bundle.documentCount());
        assertEquals(9, bundle.renderingCount());
        for (DocumentType type : List.of(DocumentType.LEAN_CANVAS, DocumentType.FINANCIAL_PLAN, DocumentType.KPI)) {
            var renderings = bundle.renderingsFor(type);
            assertEquals(3, renderings.size());
            assertTrue(startsWithMagic(renderings.get(RenderFormat.PDF), "%PDF"));
            assertTrue(startsWithMagic(renderings.get(RenderFormat.DOCX), "PK"));
            assertTrue(startsWithMagic(renderings.get(RenderFormat.PPTX), "PK"));
        }
    }

    @Test
    void export_deberiaSoportarLosTiposDelCatalogoDeExportacion() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1,
            DocumentType.LEAN_CANVAS, DocumentType.FINANCIAL_PLAN, DocumentType.MARKET_PLAN,
            DocumentType.INNOVATION_PLAN, DocumentType.ROADMAP, DocumentType.RISK_MATRIX,
            DocumentType.KPI);

        var bundle = service.export(project);

        assertEquals(7, bundle.documentCount());
        assertEquals(21, bundle.renderingCount());
    }

    @Test
    void export_conProyectoSinDocumentos_deberiaDevolverBundleVacio() {
        var now = java.time.OffsetDateTime.now();
        var project = com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.complete(
            PROJECT_ID, 1, now, now, now, java.util.List.of());

        var bundle = service.export(project);

        assertEquals(0, bundle.documentCount());
        assertEquals(0, bundle.renderingCount());
    }

    @Test
    void export_porTipo_deberiaDevolverTodosLosFormatos() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS);

        Map<RenderFormat, byte[]> renderings =
            service.export(project, DocumentType.LEAN_CANVAS);

        assertEquals(3, renderings.size());
        assertTrue(renderings.containsKey(RenderFormat.PDF));
        assertTrue(renderings.containsKey(RenderFormat.DOCX));
        assertTrue(renderings.containsKey(RenderFormat.PPTX));
    }

    @Test
    void export_porTipoYFormato_deberiaDevolverLosBytes() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS);

        byte[] pdf = service.export(project, DocumentType.LEAN_CANVAS, RenderFormat.PDF);
        byte[] docx = service.export(project, DocumentType.LEAN_CANVAS, RenderFormat.DOCX);
        byte[] pptx = service.export(project, DocumentType.LEAN_CANVAS, RenderFormat.PPTX);

        assertTrue(startsWithMagic(pdf, "%PDF"));
        assertTrue(startsWithMagic(docx, "PK"));
        assertTrue(startsWithMagic(pptx, "PK"));
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("LEAN CANVAS"));
    }

    @Test
    void export_porTipoAusente_deberiaLanzarExcepcionDeDominio() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS);

        assertThrows(EnterpriseExportException.class,
            () -> service.export(project, DocumentType.KPI));
        assertThrows(EnterpriseExportException.class,
            () -> service.export(project, DocumentType.KPI, RenderFormat.PDF));
    }

    @Test
    void export_conProyectoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> service.export(null));
        assertThrows(IllegalArgumentException.class,
            () -> service.export(null, DocumentType.KPI));
    }

    @Test
    void export_conTipoNulo_deberiaLanzar() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.KPI);
        assertThrows(IllegalArgumentException.class, () -> service.export(project, null));
    }

    @Test
    void export_conFormatoNulo_deberiaLanzar() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.KPI);
        assertThrows(IllegalArgumentException.class,
            () -> service.export(project, DocumentType.KPI, null));
    }

    @Test
    void export_conFactorySoloPdf_deberiaOmitirLosFormatosNoSoportados() {
        var soloPdf = new EnterpriseExportService(
            new EnterpriseRendererFactory(List.of(new com.kinplatform.kin.enterprise.renderer.PdfDocumentRenderer())));
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS);

        var bundle = soloPdf.export(project);
        var renderings = bundle.renderingsFor(DocumentType.LEAN_CANVAS);

        assertEquals(1, renderings.size());
        assertTrue(renderings.containsKey(RenderFormat.PDF));
        assertFalse(renderings.containsKey(RenderFormat.DOCX));
    }

    @Test
    void constructor_conFactoryNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseExportService(null));
    }

    @Test
    void export_porTipoYFormatoNoSoportado_deberiaLanzar() {
        var soloPdf = new EnterpriseExportService(
            new EnterpriseRendererFactory(List.of(new com.kinplatform.kin.enterprise.renderer.PdfDocumentRenderer())));
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS);

        assertThrows(IllegalArgumentException.class,
            () -> soloPdf.export(project, DocumentType.LEAN_CANVAS, RenderFormat.DOCX));
    }

    private boolean startsWithMagic(byte[] bytes, String magic) {
        assertNotNull(bytes);
        assertTrue(bytes.length >= magic.length());
        for (int i = 0; i < magic.length(); i++) {
            if (bytes[i] != (byte) magic.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
