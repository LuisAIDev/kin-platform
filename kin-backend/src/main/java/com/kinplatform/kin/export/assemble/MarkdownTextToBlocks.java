package com.kinplatform.kin.export.assemble;

import com.kinplatform.kin.export.model.ExportBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Convierte el texto Markdown ligero producido por los {@code SectionFormatter}
 * del reporte en bloques neutrales {@link ExportBlock}.
 *
 * <p>Transformación pura y determinista: detecta el título de la sección
 * (primera línea {@code ## }), ítems de lista ({@code - }, {@code * },
 * {@code N. }) y párrafos. No inventa contenido.</p>
 */
public final class MarkdownTextToBlocks {

    private static final Pattern NUMBERED = Pattern.compile("^\\d+[\\.\\)]\\s+");

    private MarkdownTextToBlocks() {}

    public static ConvertedSection convert(String markdown) {
        String title = null;
        List<ExportBlock> blocks = new ArrayList<>();
        List<String> currentList = null;

        String[] lines = markdown == null ? new String[0] : markdown.split("\\r?\\n", -1);
        int start = 0;
        while (start < lines.length && lines[start].isBlank()) {
            start++;
        }
        if (start < lines.length && lines[start].startsWith("## ")) {
            title = lines[start].substring(3).trim();
            start++;
        }

        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                currentList = flushList(blocks, currentList);
                continue;
            }
            String item = listItem(line);
            if (item != null) {
                if (currentList == null) {
                    currentList = new ArrayList<>();
                }
                currentList.add(item);
                continue;
            }
            currentList = flushList(blocks, currentList);
            blocks.add(ExportBlock.paragraph(line));
        }
        currentList = flushList(blocks, currentList);
        return new ConvertedSection(title, blocks);
    }

    private static List<String> flushList(List<ExportBlock> blocks, List<String> list) {
        if (list != null && !list.isEmpty()) {
            blocks.add(ExportBlock.list(list));
        }
        return new ArrayList<>();
    }

    private static String listItem(String line) {
        if (line.startsWith("- ") || line.startsWith("* ")) {
            return line.substring(2).trim();
        }
        if (NUMBERED.matcher(line).find()) {
            return NUMBERED.matcher(line).replaceFirst("").trim();
        }
        return null;
    }

    public record ConvertedSection(String title, List<ExportBlock> blocks) {}
}
