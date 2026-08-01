package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Formatea {@link ReportMetadata} a Markdown ligero.
 */
public class ReportMetadataFormatter implements SectionFormatter<ReportMetadata> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.METADATA;
    }

    @Override
    public String format(ReportMetadata section) {
        var sb = new StringBuilder();
        sb.append("## Metadata del Reporte\n\n");

        if (!section.reportVersion().isBlank()) {
            sb.append("**Versión del reporte:** ").append(section.reportVersion()).append("\n\n");
        }
        if (!section.architectureVersion().isBlank()) {
            sb.append("**Versión de arquitectura:** ").append(section.architectureVersion()).append("\n\n");
        }
        sb.append("**Generado el:** ").append(section.generatedAt().format(DATE_FORMATTER)).append("\n\n");
        if (!section.generatedBy().isBlank()) {
            sb.append("**Generado por:** ").append(section.generatedBy()).append("\n\n");
        }

        Map<String, String> engines = section.engineVersions();
        if (!engines.isEmpty()) {
            sb.append("### Versiones de Motores\n\n");
            for (var entry : engines.entrySet()) {
                sb.append("- **").append(entry.getKey()).append(":** ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("**Cobertura:** ").append(String.format(Locale.ROOT, "%.1f", section.coveragePercent())).append("%\n");
        sb.append("**Confianza:** ").append(String.format(Locale.ROOT, "%.1f", section.confidence() * 100)).append("%\n\n");

        List<String> sections = section.sectionsIncluded();
        if (!sections.isEmpty()) {
            sb.append("### Secciones Incluidas (").append(sections.size()).append(")\n\n");
            for (String s : sections) {
                sb.append("- ").append(s).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}