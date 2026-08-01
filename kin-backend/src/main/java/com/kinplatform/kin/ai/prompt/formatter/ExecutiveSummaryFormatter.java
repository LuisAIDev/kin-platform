package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

import java.util.Locale;

/**
 * Formatea {@link ExecutiveSummary} a Markdown ligero.
 */
public class ExecutiveSummaryFormatter implements SectionFormatter<ExecutiveSummary> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.EXECUTIVE;
    }

    @Override
    public String format(ExecutiveSummary section) {
        var sb = new StringBuilder();
        sb.append("## Resumen Ejecutivo\n\n");
        sb.append("**Proyecto:** ").append(section.projectName()).append("\n");
        sb.append("**Categoría:** ").append(section.projectCategory()).append("\n");
        sb.append("**Score Global:** ").append(section.overallScore()).append(" / ").append(section.maxScore()).append("\n");
        sb.append("**Viabilidad:** ").append(section.viabilityLabel()).append("\n");
        sb.append("**Cobertura:** ").append(String.format(Locale.ROOT, "%.1f", section.coveragePercent())).append("%\n\n");
        if (!section.summaryText().isBlank()) {
            sb.append(section.summaryText()).append("\n\n");
        }
        if (!section.keyHighlights().isEmpty()) {
            sb.append("**Puntos destacados:**\n");
            for (var highlight : section.keyHighlights()) {
                sb.append("- ").append(highlight).append("\n");
            }
        }
        return sb.toString();
    }
}