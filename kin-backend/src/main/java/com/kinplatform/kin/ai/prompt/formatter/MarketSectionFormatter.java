package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

/**
 * Formatea {@link MarketSection} a Markdown ligero.
 */
public class MarketSectionFormatter implements SectionFormatter<MarketSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.PROJECTION;
    }

    @Override
    public String format(MarketSection section) {
        var sb = new StringBuilder();
        sb.append("## Análisis de Mercado\n\n");

        if (!section.sector().isBlank()) {
            sb.append("**Sector:** ").append(section.sector()).append("\n\n");
        }
        if (!section.targetCustomer().isBlank()) {
            sb.append("**Cliente objetivo:** ").append(section.targetCustomer()).append("\n\n");
        }
        if (!section.city().isBlank()) {
            sb.append("**Ciudad:** ").append(section.city()).append("\n\n");
        }
        if (!section.problem().isBlank()) {
            sb.append("**Problema a resolver:** ").append(section.problem()).append("\n\n");
        }

        if (!section.coverage().isEmpty()) {
            sb.append("### Cobertura de Dimensión\n\n");
            for (var c : section.coverage()) {
                sb.append("- **").append(c.dimension().displayName()).append(":** ")
                  .append(c.covered() ? "Cubierto" : "No cubierto").append("\n");
            }
            sb.append("\n");
        }

        if (section.isEmpty()) {
            sb.append("_Sin datos de mercado disponibles._\n");
        }
        return sb.toString();
    }
}