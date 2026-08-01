package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

import java.util.Locale;

/**
 * Formatea {@link ScoresSection} a Markdown ligero.
 */
public class ScoresSectionFormatter implements SectionFormatter<ScoresSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.SCORING;
    }

    @Override
    public String format(ScoresSection section) {
        var sb = new StringBuilder();
        sb.append("## Scoring de Viabilidad\n\n");
        sb.append("**Total:** ").append(section.totalScore()).append(" / ").append(section.maxScore()).append("\n");
        sb.append("**Viabilidad:** ").append(section.viabilityLabel()).append("\n");
        sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", section.confidenceLevel())).append("%\n");
        sb.append("**Modelo:** ").append(section.scoringModelVersion()).append("\n\n");

        if (!section.categoryScores().isEmpty()) {
            sb.append("### Desglose por Categoría\n\n");
            for (var entry : section.categoryScores().entrySet()) {
                sb.append("- **").append(entry.getKey()).append(":** ")
                  .append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        if (!section.strengths().isEmpty()) {
            sb.append("### Fortalezas\n\n");
            for (var s : section.strengths()) {
                sb.append("- ").append(s).append("\n");
            }
            sb.append("\n");
        }

        if (!section.weaknesses().isEmpty()) {
            sb.append("### Debilidades\n\n");
            for (var w : section.weaknesses()) {
                sb.append("- ").append(w).append("\n");
            }
        }
        return sb.toString();
    }
}