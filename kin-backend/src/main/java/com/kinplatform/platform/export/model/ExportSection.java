package com.kinplatform.platform.export.model;

import java.util.List;

/**
 * Sección de un {@link ExportDocument}: título y bloques de contenido ordenados.
 *
 * <p>Value object inmutable.</p>
 *
 * @param title  título de la sección (nunca {@code null})
 * @param blocks bloques de la sección en orden (nunca {@code null})
 */
public record ExportSection(String title, List<ExportBlock> blocks) {

    public ExportSection {
        title = title == null ? "" : title;
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    public static ExportSection of(String title, List<ExportBlock> blocks) {
        return new ExportSection(title, blocks);
    }
}

