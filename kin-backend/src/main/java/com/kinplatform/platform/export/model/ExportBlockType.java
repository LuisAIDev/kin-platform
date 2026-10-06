package com.kinplatform.platform.export.model;

/**
 * Tipos de bloque de un {@link ExportDocument}.
 *
 * <p>Representación neutral de los elementos de un documento exportado:
 * títulos, subtítulos, párrafos, listas, tablas, texto destacado, negritas,
 * cursivas y saltos de página. Los renderizadores (DOCX/PDF/Markdown) traducen
 * cada bloque a su formato nativo.</p>
 */
public enum ExportBlockType {
    TITLE,
    SUBTITLE,
    PARAGRAPH,
    HIGHLIGHT,
    BOLD,
    ITALIC,
    LIST,
    TABLE,
    PAGE_BREAK
}

