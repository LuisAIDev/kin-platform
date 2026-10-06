package com.kinplatform.platform.export.template;

import java.util.List;

/**
 * Sección de una {@link ExportTemplate}.
 *
 * <p>Representa un elemento estructural del documento de referencia: título,
 * nivel jerárquico y sugerencias estructurales (listas, tablas) detectadas en
 * el texto extraído. Los {@code hints} sirven SOLO para el mapeo (p. ej. si la
 * sección contenía una tabla, el mapeador puede emitir una tabla con datos de
 * KIN); nunca se copian como contenido del proyecto.</p>
 *
 * @param title título de la sección (normalizado para el documento final)
 * @param level nivel jerárquico (1 = sección, 2 = subsección)
 * @param hints sugerencias estructurales detectadas (nunca {@code null})
 */
public record TemplateSection(String title, int level, List<com.kinplatform.platform.export.model.ExportBlock> hints) {

    public TemplateSection {
        title = title == null ? "" : title;
        level = Math.max(1, level);
        hints = hints == null ? List.of() : List.copyOf(hints);
    }
}


