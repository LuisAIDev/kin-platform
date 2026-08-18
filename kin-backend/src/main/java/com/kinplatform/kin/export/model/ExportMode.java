package com.kinplatform.kin.export.model;

/**
 * Modo de exportación del proyecto.
 *
 * <p>{@code COMPLETE} incluye identificación, las secciones del reporte de
 * consultoría disponibles y la información estructurada del proyecto.
 * {@code SUMMARY} incluye solo identificación, resumen ejecutivo y métricas.
 * {@code TEMPLATE} organiza los datos del proyecto según la estructura de un
 * documento de referencia (la plantilla aporta estructura, nunca contenido).</p>
 */
public enum ExportMode {
    COMPLETE,
    SUMMARY,
    TEMPLATE
}
