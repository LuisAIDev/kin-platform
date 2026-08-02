package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.CitedSource;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import com.kinplatform.kin.reporting.report.model.SourcesSection;

import java.util.Locale;

/**
 * Formatea {@link SourcesSection} a Markdown ligero (ADR-016, Etapa E5).
 *
 * <p>Frontera ADR-012 intacta: solo transforma la sección ya tipada del
 * {@code ConsultingReport}; no calcula ni decide.</p>
 */
public class SourcesSectionFormatter implements SectionFormatter<SourcesSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.SOURCES;
    }

    @Override
    public String format(SourcesSection section) {
        var sb = new StringBuilder();
        sb.append("## Fuentes Citadas\n\n");

        if (section.sources().isEmpty()) {
            sb.append("_Sin fuentes externas citadas._\n");
            return sb.toString();
        }

        int i = 1;
        for (CitedSource source : section.sources()) {
            sb.append("### ").append(i++).append(". ").append(source.claim()).append("\n\n");
            sb.append("**Fuente:** ");
            if (source.url() != null && !source.url().isBlank()) {
                sb.append(source.url());
            } else {
                sb.append(source.sourceId());
            }
            sb.append("  \n");
            sb.append("**Categoría:** ").append(source.category().displayName()).append("  \n");
            sb.append("**Relevancia:** ")
                .append(String.format(Locale.ROOT, "%.2f", source.score()))
                .append("/1.00\n\n");
        }

        sb.append("**Total:** ").append(section.sources().size()).append(" fuentes citadas\n");
        return sb.toString();
    }
}
