package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PptxDocumentRendererTest {

    private final PptxDocumentRenderer renderer = new PptxDocumentRenderer();

    @Test
    void format_deberiaSerPPTX() {
        assertEquals(RenderFormat.PPTX, renderer.format());
    }

    @Test
    void render_deberiaProducirUnPaqueteZipConLasPartesDePresentacion() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(DocumentType.LEAN_CANVAS));

        Set<String> entries = zipEntries(bytes);
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("_rels/.rels"));
        assertTrue(entries.contains("ppt/presentation.xml"));
        assertTrue(entries.contains("ppt/slides/slide1.xml"));
        assertTrue(entries.contains("ppt/slideLayouts/slideLayout1.xml"));
        assertTrue(entries.contains("ppt/slideMasters/slideMaster1.xml"));
        assertTrue(entries.contains("ppt/theme/theme1.xml"));
    }

    @Test
    void render_deberiaIncluirElContenidoEscapadoEnLaDiapositiva() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(
            DocumentType.FINANCIAL_PLAN, "CAPEX <1M> y OPEX & margen"));
        String slideXml = zipContent(bytes, "ppt/slides/slide1.xml");

        assertTrue(slideXml.contains("CAPEX &lt;1M&gt; y OPEX &amp; margen"));
        assertTrue(slideXml.contains("FINANCIAL PLAN"));
    }

    @Test
    void render_conContenidoMultilinea_deberiaIncluirTodasLasLineas() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(
            DocumentType.RISK_MATRIX, "Riesgo alto\nRiesgo medio"));
        String slideXml = zipContent(bytes, "ppt/slides/slide1.xml");

        assertTrue(slideXml.contains("Riesgo alto"));
        assertTrue(slideXml.contains("Riesgo medio"));
    }

    @Test
    void render_conLineaVacia_deberiaGenerarUnParrafoVacio() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(
            DocumentType.MARKET_PLAN, "Primera línea\n\nTercera línea"));
        String slideXml = zipContent(bytes, "ppt/slides/slide1.xml");

        assertTrue(slideXml.contains("Primera línea"));
        assertTrue(slideXml.contains("Tercera línea"));
        assertTrue(slideXml.contains("<a:p/>"));
    }

    @Test
    void render_deberiaDeclararElContenidoDePresentacion() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(DocumentType.KPI));
        String presentationXml = zipContent(bytes, "ppt/presentation.xml");
        String presentationRels = zipContent(bytes, "ppt/_rels/presentation.xml.rels");
        String contentTypes = zipContent(bytes, "[Content_Types].xml");

        assertTrue(presentationXml.contains("rId1"));
        assertTrue(presentationRels.contains("slides/slide1.xml"));
        assertTrue(contentTypes.contains("presentation.main+xml"));
    }

    @Test
    void render_conArtefactoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null));
    }

    private Set<String> zipEntries(byte[] bytes) throws IOException {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    private String zipContent(byte[] bytes, String name) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Entrada ZIP no encontrada: " + name);
    }
}
