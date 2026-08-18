package com.kinplatform.kin.export.template;

import com.kinplatform.kin.export.model.ExportBlock;
import com.kinplatform.kin.export.model.ExportTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser determinista de la estructura de un documento de referencia.
 *
 * <p>Recibe el texto extraído de un documento y produce una
 * {@link ExportTemplate} con el título y las secciones en orden. Detecta
 * títulos, secciones numeradas y por palabras clave, subsecciones, listas y
 * tablas cuando el texto extraído permite identificarlas de forma consistente.
 *
 * <p><strong>Regla de contenido:</strong> el parser solo identifica
 * ESTRUCTURA. Nunca devuelve el contenido del documento como parte del
 * proyecto. Si una tabla no puede reconstruirse de forma fiable, se degrada
 * (no se fabrican filas ni columnas).</p>
 */
public final class ProjectExportTemplateParser {

    private static final Pattern NUMBERED_HEADING = Pattern.compile("^(\\d+(?:\\.\\d+)*)[\\s.)\\-]+(.+)$");
    private static final Pattern WORD_HEADING =
            Pattern.compile("^(CAP[IÍ]TULO|SECCI[OÓ]N|PARTE|ANEXO)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALL_CAPS_LINE = Pattern.compile("[A-ZÁÉÍÓÚÜÑ0-9 .,:;/&()\\-]+");
    private static final Pattern NUMBERED_LIST = Pattern.compile("^\\d+\\)\\s+");

    private static final int MAX_HEADING_LEN = 90;

    public ExportTemplate parse(String filename, String mimeType, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException("El documento no contiene texto extraído");
        }
        String[] lines = extractedText.split("\\r?\\n", -1);
        String title = null;
        List<TemplateSection> sections = new ArrayList<>();
        String currentTitle = null;
        int currentLevel = 1;
        List<ExportBlock> currentHints = new ArrayList<>();
        List<String> currentList = null;

        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                currentList = flushList(currentHints, currentList);
                i++;
                continue;
            }
            if (title == null && !isNumberedHeading(line) && !isListLine(line) && !isTableLike(line)) {
                title = line;
                i++;
                continue;
            }
            Heading heading = heading(line);
            if (heading != null) {
                currentList = flushList(currentHints, currentList);
                if (currentTitle != null) {
                    sections.add(new TemplateSection(currentTitle, currentLevel, currentHints));
                }
                currentTitle = heading.title();
                currentLevel = heading.level();
                currentHints = new ArrayList<>();
                i++;
                continue;
            }
            if (isListLine(line)) {
                currentList = currentList == null ? new ArrayList<>() : currentList;
                currentList.add(listItem(line));
                i++;
                continue;
            }
            currentList = flushList(currentHints, currentList);
            if (isTableLike(line)) {
                List<List<String>> rows = new ArrayList<>();
                while (i < lines.length && isTableLike(lines[i].trim())) {
                    rows.add(cells(lines[i].trim()));
                    i++;
                }
                addTableHint(currentHints, rows);
                continue;
            }
            i++;
        }
        flushList(currentHints, currentList);
        if (currentTitle != null) {
            sections.add(new TemplateSection(currentTitle, currentLevel, currentHints));
        }

        return new ExportTemplate(title == null ? filenameBase(filename) : title, sections);
    }

    private String filenameBase(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private void addTableHint(List<ExportBlock> hints, List<List<String>> rows) {
        if (rows.size() < 2) {
            return;
        }
        int cols = rows.get(0).size();
        if (cols < 2) {
            return;
        }
        for (List<String> row : rows) {
            if (row.size() != cols) {
                return; // columnas inconsistentes: degradar (no fabricar)
            }
        }
        List<String> header = rows.get(0);
        List<List<String>> body = rows.subList(1, rows.size());
        hints.add(ExportBlock.table(ExportTable.of(header, body)));
    }

    private List<String> flushList(List<ExportBlock> hints, List<String> list) {
        if (list != null && !list.isEmpty()) {
            hints.add(ExportBlock.list(list));
        }
        return null;
    }

    private static String listItem(String line) {
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
            return line.substring(2).trim();
        }
        Matcher m = NUMBERED_LIST.matcher(line);
        if (m.find()) {
            return line.substring(m.end()).trim();
        }
        return line;
    }

    private static boolean isListLine(String line) {
        return line.startsWith("- ")
                || line.startsWith("* ")
                || line.startsWith("• ")
                || NUMBERED_LIST.matcher(line).find();
    }

    private static boolean isTableLike(String line) {
        return line.indexOf('\t') >= 0 || line.chars().filter(c -> c == '|').count() >= 2;
    }

    private static List<String> cells(String line) {
        if (line.indexOf('\t') >= 0) {
            return List.of(line.split("\\t", -1));
        }
        String trimmed = line;
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return java.util.Arrays.stream(trimmed.split("\\|", -1))
                .map(String::trim)
                .toList();
    }

    private static boolean isNumberedHeading(String line) {
        return NUMBERED_HEADING.matcher(line).matches();
    }

    private static Heading heading(String line) {
        Matcher numbered = NUMBERED_HEADING.matcher(line);
        if (numbered.matches()) {
            String rest = numbered.group(2).trim();
            int level = numbered.group(1).split("\\.").length;
            if (level <= 3 && (isAllCaps(rest) || rest.length() <= MAX_HEADING_LEN)) {
                return new Heading(line, level);
            }
        }
        Matcher word = WORD_HEADING.matcher(line);
        if (word.matches()) {
            return new Heading(line, 1);
        }
        if (isAllCapsLine(line)) {
            return new Heading(line, 1);
        }
        return null;
    }

    private static boolean isAllCapsLine(String line) {
        if (line.length() > MAX_HEADING_LEN || line.isBlank()) {
            return false;
        }
        if (!ALL_CAPS_LINE.matcher(line).matches()) {
            return false;
        }
        return line.chars().anyMatch(Character::isLetter);
    }

    private static boolean isAllCaps(String text) {
        return text.equals(text.toUpperCase(Locale.ROOT));
    }

    private record Heading(String title, int level) {}
}
