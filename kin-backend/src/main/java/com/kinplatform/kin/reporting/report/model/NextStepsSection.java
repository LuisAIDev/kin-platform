package com.kinplatform.kin.reporting.report.model;

import java.util.List;

/**
 * Sección de próximos pasos del reporte: agregación de los top items de
 * recomendaciones, riesgos y oportunidades, etiquetados como {@link NextStep}.
 */
public record NextStepsSection(
    List<NextStep> nextSteps
) implements ReportSection {

    public NextStepsSection {
        nextSteps = nextSteps == null ? List.of() : List.copyOf(nextSteps);
    }

    @Override
    public String sectionName() {
        return "NextSteps";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.AGGREGATE;
    }

    public boolean isEmpty() {
        return nextSteps.isEmpty();
    }

    public static NextStepsSection empty() {
        return new NextStepsSection(List.of());
    }
}
