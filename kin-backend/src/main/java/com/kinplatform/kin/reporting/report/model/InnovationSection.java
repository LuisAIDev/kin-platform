package com.kinplatform.kin.reporting.report.model;

import java.util.List;

/**
 * Sección de innovación del reporte: proyección directa de los valores de
 * dimensión de innovación y de las señales detectadas por la evaluación.
 */
public record InnovationSection(
    String solution,
    String valueProposition,
    String mvp,
    List<String> innovationSignals,
    List<DimensionCoverage> coverage
) implements ReportSection {

    public InnovationSection {
        solution = solution == null ? "" : solution;
        valueProposition = valueProposition == null ? "" : valueProposition;
        mvp = mvp == null ? "" : mvp;
        innovationSignals = innovationSignals == null ? List.of() : List.copyOf(innovationSignals);
        coverage = coverage == null ? List.of() : List.copyOf(coverage);
    }

    @Override
    public String sectionName() {
        return "Innovation";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.PROJECTION;
    }

    public boolean isEmpty() {
        return solution.isBlank() && valueProposition.isBlank() && mvp.isBlank()
            && innovationSignals.isEmpty() && coverage.isEmpty();
    }

    public static InnovationSection empty() {
        return new InnovationSection("", "", "", List.of(), List.of());
    }
}
