package com.kinplatform.platform.export.renderer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.platform.export.model.ExportBlock;
import com.kinplatform.platform.export.model.ExportDocument;
import com.kinplatform.platform.export.model.ExportSection;
import com.kinplatform.platform.export.model.ExportTable;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * Renderers DOCX/PDF/Markdown producen artefactos válidos a partir del mismo
 * modelo neutral.
 */
class ExportRenderersTest {

    private final MarkdownExportRenderer markdown = new MarkdownExportRenderer();
    private final DocxExportRenderer docx = new DocxExportRenderer();
    private final PdfExportRenderer pdf = new PdfExportRenderer();

    private ExportDocument document() {
        return new ExportDocument(
                "CAFÉ MARTE 777",
                "Proyecto KIN — Gastronomía y Alimentos",
                OffsetDateTime.parse("2026-08-17T12:00:00Z"),
                List.of(
                        ExportSection.of(
                                "Identificación del proyecto",
                                List.of(
                                        ExportBlock.paragraph("cafetería espacial orientada a astronautas"),
                                        ExportBlock.list(List.of("**Categoría:** Gastronomía y Alimentos")))),
                        ExportSection.of(
                                "Mercado",
                                List.of(ExportBlock.table(ExportTable.of(
                                        List.of("Campo", "Valor"),
                                        List.of(List.of("mercado_objetivo", "astronautas")))))),
                        ExportSection.of("Resumen", List.of(ExportBlock.highlight("Pendiente de información")))));
    }

    @Test
    void markdownEsRepresentacionLimpiaDelModelo() {
        String md = new String(markdown.render(document()), StandardCharsets.UTF_8);
        assertTrue(md.startsWith("# CAFÉ MARTE 777"));
        assertTrue(md.contains("## Identificación del proyecto"));
        assertTrue(md.contains("- **Categoría:** Gastronomía y Alimentos"));
        assertTrue(md.contains("|Campo|Valor|"));
        assertTrue(md.contains("|mercado_objetivo|astronautas|"));
        assertTrue(md.contains("> Pendiente de información"));
    }

    @Test
    void docxEsUnDocumentoWordValido() throws Exception {
        byte[] bytes = docx.render(document());
        assertTrue(bytes.length > 0);
        assertTrue(bytes[0] == 'P' && bytes[1] == 'K', "el DOCX debe ser un archivo ZIP (PK)");
        try (XWPFDocument parsed = new XWPFDocument(new java.io.ByteArrayInputStream(bytes));
                XWPFWordExtractor extractor = new XWPFWordExtractor(parsed)) {
            String text = extractor.getText();
            assertTrue(text.contains("CAFÉ MARTE 777"));
            assertTrue(text.contains("mercado_objetivo"));
        }
    }

    @Test
    void pdfEsUnDocumentoValido() {
        byte[] bytes = pdf.render(document());
        assertTrue(bytes.length > 1000);
        assertArrayEquals(
                new byte[] {'%', 'P', 'D', 'F'},
                new byte[] {bytes[0], bytes[1], bytes[2], bytes[3]},
                "el PDF debe comenzar con %PDF");
    }
}


