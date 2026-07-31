package com.kinplatform.kin.reporting.report.model;

/**
 * Taxonomía de secciones del reporte de consultoría.
 *
 * <p>Permite a los renderers y agrupaciones futuros clasificar las secciones
 * sin depender de su tipo concreto.</p>
 */
public enum ReportSectionKind {
    GENERAL,
    EXECUTIVE,
    SCORING,
    ANALYTIC,
    PROJECTION,
    AGGREGATE,
    METADATA
}
