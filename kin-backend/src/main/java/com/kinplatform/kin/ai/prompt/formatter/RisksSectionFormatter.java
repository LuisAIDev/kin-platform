package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import com.kinplatform.kin.reporting.risk.Risk;

import java.util.List;
import java.util.Locale;

/**
 * Formatea {@link RisksSection} a Markdown ligero.
 */
public class RisksSectionFormatter implements SectionFormatter<RisksSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.ANALYTIC;
    }

    @Override
    public String format(RisksSection section) {
        var sb = new StringBuilder();
        sb.append("## Análisis de Riesgos\n\n");
        sb.append("**Nivel global:** ").append(section.overallRiskLevel().name()).append("\n");
        sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", section.confidence() * 100)).append("%\n\n");

        List<Risk> risks = section.risks();
        if (risks.isEmpty()) {
            sb.append("_Sin riesgos identificados._\n");
            return sb.toString();
        }

        sb.append("### Riesgos Identificados (").append(risks.size()).append(")\n\n");

        for (int i = 0; i < risks.size(); i++) {
            Risk r = risks.get(i);
            sb.append("#### ").append(i + 1).append(". ").append(r.title()).append("\n\n");
            sb.append("**Categoría:** ").append(r.category().name()).append("  \n");
            sb.append("**Severidad:** ").append(r.severity().name()).append(" (").append(r.severityScore()).append(")  \n");
            sb.append("**Probabilidad:** ").append(r.probability().name()).append("  \n");
            sb.append("**Impacto:** ").append(r.impact().name()).append("  \n");
            sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", r.confidence() * 100)).append("%  \n");
            sb.append("**Descripción:** ").append(r.description()).append("\n\n");
            if (!r.appliedRules().isEmpty()) {
                sb.append("**Reglas aplicadas:**\n");
                for (String rule : r.appliedRules()) {
                    sb.append("- ").append(rule).append("\n");
                }
                sb.append("\n");
            }
            if (!r.explanation().reason().isBlank()) {
                sb.append("_").append(r.explanation().reason()).append("_\n\n");
            }
        }

        if (!section.topRisks().isEmpty()) {
            sb.append("### Top Riesgos (prioritarios)\n\n");
            for (int i = 0; i < section.topRisks().size(); i++) {
                Risk r = section.topRisks().get(i);
                sb.append("- **").append(r.title()).append("** (").append(r.severity().name())
                  .append(", ").append(r.probability().name()).append(")\n");
            }
        }
        return sb.toString();
    }
}