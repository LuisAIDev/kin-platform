package com.kinplatform.kin.export.template;

import java.util.List;

/**
 * Plantilla estructural de un documento de referencia (modelo neutral).
 *
 * <p>Representa únicamente la ORGANIZACIÓN del documento cargado por el
 * usuario: título y secciones en orden. Nunca contiene el contenido del
 * documento de referencia. Los datos del proyecto final provienen siempre de
 * KIN (Project, ConsultingReport, project_info).</p>
 *
 * @param title    título principal detectado en el documento de referencia
 * @param sections secciones en el orden detectado
 */
public record ExportTemplate(String title, List<TemplateSection> sections) {

    public ExportTemplate {
        title = title == null ? "" : title;
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}
