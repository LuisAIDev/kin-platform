package com.kinplatform.platform.export.model;

import java.util.List;

/**
 * Bloque tipado de un {@link ExportDocument}.
 *
 * <p>Representa una unidad de contenido neutral: título, subtítulo, párrafo,
 * texto destacado, negrita, cursiva, lista o tabla. Un {@code PAGE_BREAK} no
 * porta contenido (texto vacío).</p>
 *
 * @param type  tipo de bloque (nunca {@code null})
 * @param text  texto del bloque (para los tipos de texto; {@code null} si no aplica)
 * @param items ítems de una lista (solo para {@code LIST})
 * @param table tabla del bloque (solo para {@code TABLE})
 */
public record ExportBlock(ExportBlockType type, String text, List<String> items, ExportTable table) {

    public ExportBlock {
        text = text == null ? "" : text;
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static ExportBlock title(String text) {
        return new ExportBlock(ExportBlockType.TITLE, text, List.of(), null);
    }

    public static ExportBlock subtitle(String text) {
        return new ExportBlock(ExportBlockType.SUBTITLE, text, List.of(), null);
    }

    public static ExportBlock paragraph(String text) {
        return new ExportBlock(ExportBlockType.PARAGRAPH, text, List.of(), null);
    }

    public static ExportBlock highlight(String text) {
        return new ExportBlock(ExportBlockType.HIGHLIGHT, text, List.of(), null);
    }

    public static ExportBlock bold(String text) {
        return new ExportBlock(ExportBlockType.BOLD, text, List.of(), null);
    }

    public static ExportBlock italic(String text) {
        return new ExportBlock(ExportBlockType.ITALIC, text, List.of(), null);
    }

    public static ExportBlock list(List<String> items) {
        return new ExportBlock(ExportBlockType.LIST, "", items, null);
    }

    public static ExportBlock table(ExportTable table) {
        return new ExportBlock(ExportBlockType.TABLE, "", List.of(), table);
    }

    public static ExportBlock pageBreak() {
        return new ExportBlock(ExportBlockType.PAGE_BREAK, "", List.of(), null);
    }
}

