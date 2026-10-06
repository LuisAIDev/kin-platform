package com.kinplatform.platform.export.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Documento exportado del proyecto (modelo neutral).
 *
 * <p>Representa el contenido completo de un proyecto de KIN independiente del
 * formato de salida: portada (título/subtítulo/fecha) y secciones ordenadas.
 * La fuente principal son los datos del proyecto (Project, ConsultingReport,
 * project_info); el historial de chat solo puede complementar, nunca ser la
 * base.</p>
 *
 * @param title      título del documento (nombre del proyecto)
 * @param subtitle   subtítulo de la portada
 * @param generatedAt instante de generación
 * @param sections   secciones en orden de aparición
 */
public record ExportDocument(String title, String subtitle, OffsetDateTime generatedAt, List<ExportSection> sections) {

    public ExportDocument {
        title = title == null ? "" : title;
        subtitle = subtitle == null ? "" : subtitle;
        generatedAt = generatedAt == null ? OffsetDateTime.now() : generatedAt;
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}

