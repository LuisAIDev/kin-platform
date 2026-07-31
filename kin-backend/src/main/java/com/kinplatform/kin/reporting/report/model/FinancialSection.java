package com.kinplatform.kin.reporting.report.model;

import java.util.List;

/**
 * Sección financiera del reporte: proyección directa de los valores de
 * dimensión financiera ya presentes en el contexto. Sin cálculo de negocio.
 */
public record FinancialSection(
    String revenueModel,
    String resources,
    String objectives,
    List<DimensionCoverage> coverage
) implements ReportSection {

    public FinancialSection {
        revenueModel = revenueModel == null ? "" : revenueModel;
        resources = resources == null ? "" : resources;
        objectives = objectives == null ? "" : objectives;
        coverage = coverage == null ? List.of() : List.copyOf(coverage);
    }

    @Override
    public String sectionName() {
        return "Financial";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.PROJECTION;
    }

    public boolean isEmpty() {
        return revenueModel.isBlank() && resources.isBlank() && objectives.isBlank()
            && coverage.isEmpty();
    }

    public static FinancialSection empty() {
        return new FinancialSection("", "", "", List.of());
    }
}
