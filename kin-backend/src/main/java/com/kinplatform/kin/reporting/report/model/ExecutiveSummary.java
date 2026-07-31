package com.kinplatform.kin.reporting.report.model;

import java.util.List;

/**
 * Resumen ejecutivo del reporte de consultoría: identidad del proyecto,
 * score global y los puntos destacados.
 */
public record ExecutiveSummary(
    String projectName,
    String projectCategory,
    int overallScore,
    int maxScore,
    String viabilityLabel,
    double coveragePercent,
    String summaryText,
    List<String> keyHighlights
) implements ReportSection {

    public ExecutiveSummary {
        projectName = projectName == null ? "" : projectName;
        projectCategory = projectCategory == null ? "" : projectCategory;
        overallScore = Math.max(0, overallScore);
        maxScore = Math.max(0, maxScore);
        viabilityLabel = viabilityLabel == null ? "" : viabilityLabel;
        coveragePercent = Math.max(0.0, Math.min(100.0, coveragePercent));
        summaryText = summaryText == null ? "" : summaryText;
        keyHighlights = keyHighlights == null ? List.of() : List.copyOf(keyHighlights);
    }

    @Override
    public String sectionName() {
        return "ExecutiveSummary";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.EXECUTIVE;
    }

    public boolean isEmpty() {
        return projectName.isBlank() && projectCategory.isBlank() && overallScore == 0
            && maxScore == 0 && viabilityLabel.isBlank() && summaryText.isBlank()
            && keyHighlights.isEmpty();
    }

    public static ExecutiveSummary empty() {
        return new ExecutiveSummary("", "", 0, 0, "", 0.0, "", List.of());
    }
}
