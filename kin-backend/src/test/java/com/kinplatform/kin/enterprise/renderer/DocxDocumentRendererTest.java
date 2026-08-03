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

class DocxDocumentRendererTest {

    private final DocxDocumentRenderer renderer = new DocxDocumentRenderer();

    @Test
    void format_deberiaSerDOCX() {
        assertEquals(RenderFormat.DOCX, renderer.format());
    }

    @Test
    void render_deberiaProducirUnPaqueteZipConLasPartesWord() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(DocumentType.LEAN_CANVAS));

        assertTrue(bytes.length > 0);
        Set<String> entries = zipEntries(bytes);
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("_rels/.rels"));
        assertTrue(entries.contains("word/document.xml"));
        assertTrue(entries.contains("docProps/core.xml"));
    }

    @Test
    void render_deberiaIncluirElContenidoEscapadoEnElDocumento() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(
            DocumentType.KPI, "Margen <bruto> y renta & neta"));
        String documentXml = zipContent(bytes, "word/document.xml");

        assertTrue(documentXml.contains("Margen &lt;bruto&gt; y renta &amp; neta"));
        assertTrue(documentXml.contains("KPI"));
    }

    @Test
    void render_conContenidoMultilinea_deberiaGenerarUnParrafoPorLinea() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(
            DocumentType.ROADMAP, "Fase uno\nFase dos\nFase tres"));
        String documentXml = zipContent(bytes, "word/document.xml");

        int paragraphs = countOccurrences(documentXml, "<w:p>");
        assertTrue(paragraphs >= 5, "Título, línea en blanco y las tres fases");
    }

    @Test
    void render_deberiaIncluirElTituloEnLasPropiedades() throws IOException {
        byte[] bytes = renderer.render(RendererTestFixtures.document(DocumentType.MARKET_PLAN));
        String coreXml = zipContent(bytes, "docProps/core.xml");

        assertTrue(coreXml.contains("MARKET PLAN"));
        assertTrue(coreXml.contains("KIN Platform"));
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

    private int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
