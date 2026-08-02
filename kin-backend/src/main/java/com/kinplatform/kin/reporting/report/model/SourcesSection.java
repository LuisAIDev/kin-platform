package com.kinplatform.kin.reporting.report.model;

import java.util.List;

/**
 * Sección de fuentes citadas del reporte (ADR-016, Etapa E5): las fuentes
 * externas verificadas que sustentan el análisis. Es la 11.ª sección del
 * {@link ConsultingReport}.
 *
 * <p>Aditiva: cuando no hay fuentes, {@code SourcesSection.empty()} se omite
 * del reporte (el reporte se comporta exactamente como antes de la Fase 8).</p>
 */
public record SourcesSection(
    List<CitedSource> sources
) implements ReportSection {

    public SourcesSection {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    @Override
    public String sectionName() {
        return "Sources";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.SOURCES;
    }

    public boolean isEmpty() {
        return sources.isEmpty();
    }

    public static SourcesSection empty() {
        return new SourcesSection(List.of());
    }
}
