package com.kinplatform.kin.reporting.report.model;

import java.util.List;

/**
 * Sección de mercado del reporte: proyección directa de los valores de
 * dimensión de mercado ya presentes en el contexto. Sin estimaciones.
 */
public record MarketSection(
    String sector,
    String targetCustomer,
    String city,
    String problem,
    List<DimensionCoverage> coverage
) implements ReportSection {

    public MarketSection {
        sector = sector == null ? "" : sector;
        targetCustomer = targetCustomer == null ? "" : targetCustomer;
        city = city == null ? "" : city;
        problem = problem == null ? "" : problem;
        coverage = coverage == null ? List.of() : List.copyOf(coverage);
    }

    @Override
    public String sectionName() {
        return "Market";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.PROJECTION;
    }

    public boolean isEmpty() {
        return sector.isBlank() && targetCustomer.isBlank() && city.isBlank()
            && problem.isBlank() && coverage.isEmpty();
    }

    public static MarketSection empty() {
        return new MarketSection("", "", "", "", List.of());
    }
}
