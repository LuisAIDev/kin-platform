package com.kinplatform.kin.export.renderer;

import com.kinplatform.kin.export.model.ExportBlock;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.kin.export.model.ExportTable;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renderizador PDF del {@link ExportDocument} usando OpenPDF.
 *
 * <p>Genera un PDF multipágina, legible y profesional con portada, títulos,
 * párrafos, listas, tablas, separación por secciones y pie de página. El
 * contenido proviene del modelo neutral (no del historial de chat).</p>
 */
public final class PdfExportRenderer implements ExportRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color ACCENT = new Color(22, 101, 52);

    @Override
    public byte[] render(ExportDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("document no puede ser null");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document pdf = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter writer = PdfWriter.getInstance(pdf, out);
            writer.setPageEvent(new FooterEvent("Generado por KIN Platform — " + document.title()));
            pdf.open();
            renderCover(pdf, document);
            for (ExportSection section : document.sections()) {
                renderSection(pdf, section);
            }
            pdf.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF", e);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private void renderCover(Document pdf, ExportDocument document) throws DocumentException {
        Paragraph title = new Paragraph(document.title(), font(26, Font.BOLD, null));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(16);
        pdf.add(title);
        if (!document.subtitle().isBlank()) {
            Paragraph subtitle = new Paragraph(document.subtitle(), font(14, Font.ITALIC, null));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16);
            pdf.add(subtitle);
        }
        Paragraph meta = new Paragraph(
                "Generado: " + document.generatedAt().format(DATE), font(10, Font.NORMAL, new Color(120, 120, 120)));
        meta.setAlignment(Element.ALIGN_CENTER);
        pdf.add(meta);
        pdf.newPage();
    }

    private void renderSection(Document pdf, ExportSection section) throws DocumentException {
        Paragraph heading = new Paragraph(section.title(), font(16, Font.BOLD, ACCENT));
        heading.setSpacingBefore(18);
        heading.setSpacingAfter(10);
        pdf.add(heading);
        for (ExportBlock block : section.blocks()) {
            renderBlock(pdf, block);
        }
    }

    private void renderBlock(Document pdf, ExportBlock block) throws DocumentException {
        switch (block.type()) {
            case TITLE -> addText(pdf, block.text(), 18, Font.BOLD, null, 12);
            case SUBTITLE -> addText(pdf, block.text(), 13, Font.BOLD, null, 8);
            case PARAGRAPH -> addRich(pdf, block.text(), 11, Font.NORMAL, null, 8);
            case HIGHLIGHT -> addText(pdf, block.text(), 11, Font.ITALIC, new Color(100, 100, 100), 8);
            case BOLD -> addText(pdf, block.text(), 11, Font.BOLD, null, 8);
            case ITALIC -> addText(pdf, block.text(), 11, Font.ITALIC, null, 8);
            case LIST -> renderList(pdf, block);
            case TABLE -> renderTable(pdf, block.table());
            case PAGE_BREAK -> pdf.newPage();
            default -> {}
        }
    }

    private void addText(Document pdf, String text, int size, int style, Color color, float after)
            throws DocumentException {
        Paragraph p = new Paragraph(paragraph(text), font(size, style, color));
        p.setSpacingAfter(after);
        pdf.add(p);
    }

    private void addRich(Document pdf, String text, int size, int style, Color color, float after)
            throws DocumentException {
        Paragraph p = new Paragraph();
        p.setSpacingAfter(after);
        for (Phrase part : phrases(text, size, style, color)) {
            p.add(part);
        }
        pdf.add(p);
    }

    private void renderList(Document pdf, ExportBlock block) throws DocumentException {
        for (String item : block.items()) {
            Paragraph p = new Paragraph();
            p.setIndentationLeft(20);
            p.setSpacingAfter(4);
            p.add(new Chunk("•  ", font(11, Font.BOLD, ACCENT)));
            p.add(new Chunk(item, font(11, Font.NORMAL, null)));
            pdf.add(p);
        }
    }

    private void renderTable(Document pdf, ExportTable table) throws DocumentException {
        if (table == null || table.isEmpty()) {
            return;
        }
        int cols = Math.max(1, table.header().size());
        PdfPTable pdfTable = new PdfPTable(cols);
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(6);
        pdfTable.setSpacingAfter(12);
        for (String header : table.header()) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font(10, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(ACCENT);
            cell.setPadding(4);
            pdfTable.addCell(cell);
        }
        for (List<String> row : table.rows()) {
            for (int c = 0; c < cols; c++) {
                String value = c < row.size() ? row.get(c) : "";
                PdfPCell cell = new PdfPCell(new Phrase(value, font(9, Font.NORMAL, null)));
                cell.setPadding(3);
                pdfTable.addCell(cell);
            }
        }
        pdf.add(pdfTable);
    }

    private List<Phrase> phrases(String text, int size, int style, Color color) {
        List<Phrase> phrases = new ArrayList<>();
        String[] parts = text.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            boolean bold = i % 2 != 0;
            Font f = font(size, bold ? Font.BOLD : style, color);
            phrases.add(new Phrase(new Chunk(parts[i], f)));
        }
        return phrases;
    }

    private String paragraph(String text) {
        return text == null ? "" : text.replace("**", "");
    }

    private Font font(int size, int style, Color color) {
        Font f;
        try {
            f = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style);
        } catch (Exception e) {
            // Fallback if font embedding fails
            f = FontFactory.getFont(FontFactory.HELVETICA, size, style);
        }
        if (color != null) {
            f.setColor(color);
        }
        return f;
    }

    private static final class FooterEvent extends PdfPageEventHelper {
        private final String text;

        FooterEvent(String text) {
            this.text = text;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Font footerFont;
            try {
                footerFont = FontFactory.getFont(FontFactory.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.NORMAL, new Color(150, 150, 150));
            } catch (Exception e) {
                footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(150, 150, 150));
            }
            Phrase phrase = new Phrase(text, footerFont);
            ColumnText.showTextAligned(
                    canvas, Element.ALIGN_CENTER, phrase, (document.left() + document.right()) / 2f, 28, 0);
            Phrase page = new Phrase(String.valueOf(writer.getPageNumber()), footerFont);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, page, document.right(), 28, 0);
        }
    }
}
