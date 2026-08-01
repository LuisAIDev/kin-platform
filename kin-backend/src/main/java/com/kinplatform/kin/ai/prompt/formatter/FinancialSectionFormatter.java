package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

/**
 * Formatea {@link FinancialSection} a Markdown ligero.
 */
public class FinancialSectionFormatter implements SectionFormatter<FinancialSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.PROJECTION;
    }

    @Override
    public String format(FinancialSection section) {
        var sb = new StringBuilder();
        sb.append("## Proyección Financiera\n\n");

        if (!section.revenueModel().isBlank()) {
            sb.append("**Modelo de ingresos:** ").append(section.revenueModel()).append("\n\n");
        }
        if (!section.resources().isBlank()) {
            sb.append("**Recursos:** ").append(section.resources()).append("\n\n");
        }
        if (!section.objectives().isBlank()) {
            sb.append("**Objetivos financieros:** ").append(section.objectives()).append("\n\n");
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
            sb.append("_Sin datos financieros disponibles._\n");
        }
        return sb.toString();
    }
}