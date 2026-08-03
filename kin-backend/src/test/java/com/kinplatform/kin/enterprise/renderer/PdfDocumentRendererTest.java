package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentRendererTest {

    private final PdfDocumentRenderer renderer = new PdfDocumentRenderer();

    @Test
    void format_deberiaSerPDF() {
        assertEquals(RenderFormat.PDF, renderer.format());
    }

    @Test
    void render_deberiaProducirUnPdfValido() {
        byte[] bytes = renderer.render(RendererTestFixtures.document(DocumentType.LEAN_CANVAS));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.contains("startxref"));
        assertTrue(pdf.trim().endsWith("%%EOF"));
        assertTrue(pdf.contains("xref"));
        assertTrue(pdf.contains("/Type /Catalog"));
        assertTrue(pdf.contains("/BaseFont /Helvetica"));
    }

    @Test
    void render_deberiaIncluirElContenidoDelArtefacto() {
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.KPI, "Objetivo: reducir costes"));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.contains("Objetivo: reducir costes"));
        assertTrue(pdf.contains("KPI"));
    }

    @Test
    void render_deberiaPreservarLosCaracteresLatin1() {
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.FINANCIAL_PLAN, "Margen óptimo del 30%"));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.contains("óptimo"));
    }

    @Test
    void render_deberiaEscaparLosCaracteresEspeciales() {
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.MARKET_PLAN, "(nota) y \\ruta"));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.contains("\\("));
        assertTrue(pdf.contains("\\)"));
    }

    @Test
    void render_conLineaLarga_deberiaAjustarElTexto() {
        String longLine = "una linea extremadamente larga ".repeat(10);
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.ROADMAP, longLine));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        int tjCount = countOccurrences(pdf, "Tj");
        assertTrue(tjCount >= 3, "La línea larga debería dividirse en varias líneas de texto");
    }

    @Test
    void render_conCaracteresFueraDeLatin1_deberiaSustituirlos() {
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.INNOVATION_PLAN, "Mercado € global"));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.contains("Mercado ? global"));
    }

    @Test
    void render_conCaracteresDeControl_deberiaSustituirlosPorEspacio() {
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.KPI, "a\tb"));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.contains("a b"));
    }

    @Test
    void render_conArtefactoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(null));
    }

    @Test
    void render_conContenidoMultilinea_deberiaIncluirTodasLasLineas() {
        byte[] bytes = renderer.render(
            RendererTestFixtures.document(DocumentType.RISK_MATRIX,
                "Riesgo alto\nRiesgo medio\nRiesgo bajo"));
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.contains("Riesgo alto"));
        assertTrue(pdf.contains("Riesgo medio"));
        assertTrue(pdf.contains("Riesgo bajo"));
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
