package com.kinplatform.kin.reporting.report.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Metadata del reporte de consultoría: versiones, momento de generación,
 * cobertura, confianza y las secciones incluidas.
 *
 * <p>{@code sectionsIncluded} y {@code generatedAt} los deriva
 * {@link ReportBuilder#build()} de las secciones ensambladas.</p>
 */
public record ReportMetadata(
    String reportVersion,
    String architectureVersion,
    OffsetDateTime generatedAt,
    String generatedBy,
    Map<String, String> engineVersions,
    double coveragePercent,
    double confidence,
    List<String> sectionsIncluded
) implements ReportSection {

    public ReportMetadata {
        reportVersion = reportVersion == null ? "" : reportVersion;
        architectureVersion = architectureVersion == null ? "" : architectureVersion;
        generatedAt = generatedAt == null ? OffsetDateTime.now() : generatedAt;
        generatedBy = generatedBy == null ? "" : generatedBy;
        engineVersions = engineVersions == null ? Map.of() : Map.copyOf(engineVersions);
        coveragePercent = Math.max(0.0, Math.min(100.0, coveragePercent));
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        sectionsIncluded = sectionsIncluded == null ? List.of() : List.copyOf(sectionsIncluded);
    }

    @Override
    public String sectionName() {
        return "ReportMetadata";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.METADATA;
    }

    public ReportMetadata withGeneratedAt(OffsetDateTime generatedAt) {
        return new ReportMetadata(reportVersion, architectureVersion, generatedAt, generatedBy,
            engineVersions, coveragePercent, confidence, sectionsIncluded);
    }

    public ReportMetadata withSectionsIncluded(List<String> sectionsIncluded) {
        return new ReportMetadata(reportVersion, architectureVersion, generatedAt, generatedBy,
            engineVersions, coveragePercent, confidence, sectionsIncluded);
    }

    public static ReportMetadata empty() {
        return new ReportMetadata("", "", OffsetDateTime.now(), "", Map.of(), 0.0, 0.0, List.of());
    }
}
