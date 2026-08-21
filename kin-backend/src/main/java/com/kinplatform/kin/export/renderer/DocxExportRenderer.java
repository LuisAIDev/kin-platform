package com.kinplatform.kin.export.renderer;

import com.kinplatform.kin.export.model.ExportBlock;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.kin.export.model.ExportTable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * Renderizador DOCX del {@link ExportDocument} usando Apache POI XWPF.
 *
 * <p>Genera un documento Word real y editable (portada, títulos, subtítulos,
 * párrafos, listas, tablas, negritas, saltos de página y pie de página).</p>
 */
public final class DocxExportRenderer implements ExportRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public byte[] render(ExportDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("document no puede ser null");
        }
        try (XWPFDocument doc = new XWPFDocument()) {
            renderCover(doc, document);
            for (ExportSection section : document.sections()) {
                renderSection(doc, section);
            }
            renderFooter(doc, document);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                doc.write(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el DOCX", e);
        }
    }

    private void renderCover(XWPFDocument doc, ExportDocument document) {
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        title.setSpacingAfter(200);
        addRuns(title, document.title(), 26, true, false);
        if (!document.subtitle().isBlank()) {
            XWPFParagraph subtitle = doc.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            subtitle.setSpacingAfter(200);
            addRuns(subtitle, document.subtitle(), 14, false, true);
        }
        XWPFParagraph meta = doc.createParagraph();
        meta.setAlignment(ParagraphAlignment.CENTER);
        meta.setSpacingAfter(400);
        addRuns(meta, "Generado: " + document.generatedAt().format(DATE), 10, false, false);
        pageBreak(doc);
    }

    private void renderSection(XWPFDocument doc, ExportSection section) {
        if (!section.title().isBlank()) {
            XWPFParagraph heading = doc.createParagraph();
            heading.setSpacingBefore(240);
            heading.setSpacingAfter(120);
            addRuns(heading, section.title(), 16, true, false);
        }
        for (ExportBlock block : section.blocks()) {
            renderBlock(doc, block);
        }
    }

    private void renderBlock(XWPFDocument doc, ExportBlock block) {
        switch (block.type()) {
            case TITLE -> heading(doc, block.text(), 18, true, false);
            case SUBTITLE -> heading(doc, block.text(), 13, true, false);
            case PARAGRAPH -> paragraph(doc, block.text(), 11, false, false);
            case HIGHLIGHT -> heading(doc, block.text(), 11, false, true);
            case BOLD -> paragraph(doc, block.text(), 11, true, false);
            case ITALIC -> paragraph(doc, block.text(), 11, false, true);
            case LIST -> list(doc, block);
            case TABLE -> table(doc, block.table());
            case PAGE_BREAK -> pageBreak(doc);
            default -> {}
        }
    }

    private void heading(XWPFDocument doc, String text, int size, boolean bold, boolean italic) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(80);
        addRuns(p, text, size, bold, italic);
    }

    private void paragraph(XWPFDocument doc, String text, int size, boolean bold, boolean italic) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(120);
        addRuns(p, text, size, bold, italic);
    }

    private void list(XWPFDocument doc, ExportBlock block) {
        for (String item : block.items()) {
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationLeft(360);
            p.setSpacingAfter(40);
            addRuns(p, "• " + item, 11, false, false);
        }
    }

    private void table(XWPFDocument doc, ExportTable table) {
        if (table == null || table.isEmpty()) {
            return;
        }
        int rows = table.rows().size() + 1;
        int cols = Math.max(1, table.header().size());
        XWPFTable xwpfTable = doc.createTable(rows, cols);
        xwpfTable.setWidth("100%");
        for (int c = 0; c < table.header().size(); c++) {
            setCell(xwpfTable.getRow(0), c, table.header().get(c), true);
        }
        for (int r = 0; r < table.rows().size(); r++) {
            List<String> row = table.rows().get(r);
            for (int c = 0; c < row.size(); c++) {
                setCell(xwpfTable.getRow(r + 1), c, row.get(c), false);
            }
        }
        XWPFParagraph spacer = doc.createParagraph();
        spacer.setSpacingAfter(120);
    }

    private void setCell(XWPFTableRow row, int c, String text, boolean bold) {
        if (row.getCell(c) == null) {
            row.addNewTableCell();
        }
        var cell = row.getCell(c);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        addRuns(p, text, 10, bold, false);
    }

    private void pageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.addBreak(BreakType.PAGE);
    }

    private void renderFooter(XWPFDocument doc, ExportDocument document) {
        try {
            var footer = doc.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            XWPFParagraph p = footer.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            addRuns(
                    p,
                    "Generado por KIN Platform — " + document.title() + " — "
                            + document.generatedAt().format(DATE),
                    8,
                    false,
                    true);
        } catch (RuntimeException ignored) {
            // el pie de página es decorativo; no bloquea la generación
        }
    }

    private void addRuns(XWPFParagraph p, String text, int size, boolean bold, boolean italic) {
        for (Segment segment : splitBold(text)) {
            XWPFRun run = p.createRun();
            run.setText(segment.text());
            run.setFontSize(size);
            run.setBold(bold || segment.isBold());
            run.setItalic(italic || segment.isItalic());
            run.setFontFamily("Calibri");
        }
    }

    private List<Segment> splitBold(String text) {
        List<Segment> segments = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        String[] parts = text.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            boolean bold = i % 2 != 0;
            segments.add(new Segment(parts[i], bold, false));
        }
        if (segments.isEmpty()) {
            segments.add(new Segment(text, false, false));
        }
        return segments;
    }

    private record Segment(String text, boolean isBold, boolean isItalic) {}
}
