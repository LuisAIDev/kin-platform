package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import com.kinplatform.kin.reporting.opportunity.Opportunity;

import java.util.List;
import java.util.Locale;

/**
 * Formatea {@link OpportunitiesSection} a Markdown ligero.
 */
public class OpportunitiesSectionFormatter implements SectionFormatter<OpportunitiesSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.ANALYTIC;
    }

    @Override
    public String format(OpportunitiesSection section) {
        var sb = new StringBuilder();
        sb.append("## Oportunidades Identificadas\n\n");
        sb.append("**Total:** ").append(section.opportunities().size()).append("\n");
        sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", section.confidence() * 100)).append("%\n\n");

        List<Opportunity> opps = section.opportunities();
        if (opps.isEmpty()) {
            sb.append("_Sin oportunidades identificadas._\n");
            return sb.toString();
        }

        for (int i = 0; i < opps.size(); i++) {
            Opportunity o = opps.get(i);
            sb.append("### ").append(i + 1).append(". ").append(o.title()).append("\n\n");
            sb.append("**Categoría:** ").append(o.category().name()).append("  \n");
            sb.append("**Prioridad:** ").append(o.priority()).append("/10  \n");
            sb.append("**Impacto:** ").append(o.impactLevel().name()).append("  \n");
            sb.append("**Esfuerzo:** ").append(o.effortLevel().name()).append("  \n");
            sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", o.confidence() * 100)).append("%  \n");
            sb.append("**Descripción:** ").append(o.description()).append("\n\n");
            if (!o.appliedRules().isEmpty()) {
                sb.append("**Reglas aplicadas:**\n");
                for (String rule : o.appliedRules()) {
                    sb.append("- ").append(rule).append("\n");
                }
                sb.append("\n");
            }
            if (!o.explanation().reason().isBlank()) {
                sb.append("_").append(o.explanation().reason()).append("_\n\n");
            }
        }

        if (!section.topOpportunities().isEmpty()) {
            sb.append("### Top Oportunidades (prioritarias)\n\n");
            for (int i = 0; i < section.topOpportunities().size(); i++) {
                Opportunity o = section.topOpportunities().get(i);
                sb.append("- **").append(o.title()).append("** (prioridad ")
                  .append(o.priority()).append(", impacto ").append(o.impactLevel().name()).append(")\n");
            }
        }
        return sb.toString();
    }
}