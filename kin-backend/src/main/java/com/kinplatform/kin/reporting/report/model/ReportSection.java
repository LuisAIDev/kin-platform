package com.kinplatform.kin.reporting.report.model;

/**
 * Sección tipada de un {@link ConsultingReport}.
 *
 * <p>Toda sección aporta su nombre ({@code sectionName()}) como única fuente
 * de verdad del identificador y una taxonomía ({@code kind()}) para que los
 * renderers y agrupaciones futuros la clasifiquen sin switches sobre tipos.</p>
 */
public interface ReportSection {

    String sectionName();

    default ReportSectionKind kind() {
        return ReportSectionKind.GENERAL;
    }
}
