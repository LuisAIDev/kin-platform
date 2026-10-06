package com.kinplatform.platform.export.application;

import java.util.List;

/**
 * Opciones disponibles de exportación de un proyecto (respuesta read-only).
 *
 * @param formats        formatos soportados (DOCX, PDF, MARKDOWN)
 * @param hasReport      si el proyecto tiene un reporte de consultoría generado
 * @param reportSections secciones presentes en el reporte (vacío si no hay)
 * @param infoSections   secciones de información estructurada del proyecto
 * @param filenameBase   base del nombre de archivo sugerido
 */
public record ExportOptions(
        List<String> formats,
        boolean hasReport,
        List<String> reportSections,
        List<String> infoSections,
        String filenameBase) {}

