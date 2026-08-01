package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

import java.util.List;
import java.util.Locale;

/**
 * Formatea {@link RecommendationsSection} a Markdown ligero.
 */
public class RecommendationsSectionFormatter implements SectionFormatter<RecommendationsSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.ANALYTIC;
    }

    @Override
    public String format(RecommendationsSection section) {
        var sb = new StringBuilder();
        sb.append("## Recomendaciones\n\n");
        sb.append("**Prioridad global:** ").append(section.priority()).append("/10\n");
        sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", section.confidence() * 100)).append("%\n");
        sb.append("**Categoría dominante:** ").append(section.dominantCategory().name()).append("\n\n");

        List<Recommendation> recs = section.recommendations();
        if (recs.isEmpty()) {
            sb.append("_Sin recomendaciones generadas._\n");
            return sb.toString();
        }

        for (int i = 0; i < recs.size(); i++) {
            Recommendation r = recs.get(i);
            sb.append("### ").append(i + 1).append(". ").append(r.title()).append("\n\n");
            sb.append("**Categoría:** ").append(r.category().name()).append("  \n");
            sb.append("**Prioridad:** ").append(r.priority()).append("/10  \n");
            sb.append("**Impacto:** ").append(r.impactLevel().name()).append("  \n");
            sb.append("**Esfuerzo:** ").append(r.effortLevel().name()).append("  \n");
            sb.append("**Descripción:** ").append(r.description()).append("\n\n");
            if (!r.actionableSteps().isEmpty()) {
                sb.append("**Pasos accionables:**\n");
                for (String step : r.actionableSteps()) {
                    sb.append("- ").append(step).append("\n");
                }
                sb.append("\n");
            }
            sb.append("**Resultado esperado:** ").append(r.expectedOutcome()).append("\n\n");
            if (!r.explanation().reason().isBlank()) {
                sb.append("_").append(r.explanation().reason()).append("_\n\n");
            }
        }
        return sb.toString();
    }
}