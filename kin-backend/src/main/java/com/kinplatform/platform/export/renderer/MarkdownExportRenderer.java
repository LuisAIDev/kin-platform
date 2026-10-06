package com.kinplatform.platform.export.renderer;

import com.kinplatform.platform.export.model.ExportBlock;
import com.kinplatform.platform.export.model.ExportBlockType;
import com.kinplatform.platform.export.model.ExportDocument;
import com.kinplatform.platform.export.model.ExportSection;
import com.kinplatform.platform.export.model.ExportTable;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renderizador Markdown del {@link ExportDocument}.
 *
 * <p>Representación limpia y determinista del mismo modelo neutral que DOCX y
 * PDF (no existe lógica independiente que produzca información diferente).</p>
 */
public final class MarkdownExportRenderer implements ExportRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public byte[] render(ExportDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("document no puede ser null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(document.title()).append('\n');
        if (!document.subtitle().isBlank()) {
            sb.append("\n*").append(document.subtitle()).append("*\n");
        }
        sb.append('\n');
        sb.append("Generado: ").append(document.generatedAt().format(DATE)).append("\n\n---\n\n");

        for (ExportSection section : document.sections()) {
            sb.append("## ").append(section.title()).append("\n\n");
            for (ExportBlock block : section.blocks()) {
                renderBlock(sb, block);
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void renderBlock(StringBuilder sb, ExportBlock block) {
        ExportBlockType type = block.type();
        switch (type) {
            case TITLE -> sb.append("# ").append(block.text()).append("\n\n");
            case SUBTITLE -> sb.append("### ").append(block.text()).append("\n\n");
            case PARAGRAPH -> sb.append(block.text()).append("\n\n");
            case HIGHLIGHT -> sb.append("> ").append(block.text()).append("\n\n");
            case BOLD -> sb.append("**").append(block.text()).append("**\n\n");
            case ITALIC -> sb.append("*").append(block.text()).append("*\n\n");
            case LIST -> {
                for (String item : block.items()) {
                    sb.append("- ").append(item).append('\n');
                }
                sb.append('\n');
            }
            case TABLE -> renderTable(sb, block.table());
            case PAGE_BREAK -> sb.append("---\n\n");
            default -> {}
        }
    }

    private void renderTable(StringBuilder sb, ExportTable table) {
        if (table == null || table.isEmpty()) {
            return;
        }
        sb.append('|').append(String.join("|", table.header())).append("|\n");
        sb.append('|').append("---|".repeat(table.header().size())).append('\n');
        for (List<String> row : table.rows()) {
            sb.append('|').append(String.join("|", row)).append("|\n");
        }
        sb.append('\n');
    }
}


