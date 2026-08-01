package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

import java.util.List;

/**
 * Formatea {@link InnovationSection} a Markdown ligero.
 */
public class InnovationSectionFormatter implements SectionFormatter<InnovationSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.PROJECTION;
    }

    @Override
    public String format(InnovationSection section) {
        var sb = new StringBuilder();
        sb.append("## Innovación\n\n");

        if (!section.solution().isBlank()) {
            sb.append("**Solución:** ").append(section.solution()).append("\n\n");
        }
        if (!section.valueProposition().isBlank()) {
            sb.append("**Propuesta de valor:** ").append(section.valueProposition()).append("\n\n");
        }
        if (!section.mvp().isBlank()) {
            sb.append("**MVP:** ").append(section.mvp()).append("\n\n");
        }

        List<String> signals = section.innovationSignals();
        if (!signals.isEmpty()) {
            sb.append("### Señales de Innovación\n\n");
            for (int i = 0; i < signals.size(); i++) {
                sb.append("- ").append(signals.get(i)).append("\n");
            }
            sb.append("\n");
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
            sb.append("_Sin datos de innovación disponibles._\n");
        }
        return sb.toString();
    }
}