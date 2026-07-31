package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskLevel;

import java.util.List;

/**
 * Sección de riesgos del reporte: reutiliza el VO {@link Risk} y los top
 * riesgos ya calculados por el RiskEngine.
 */
public record RisksSection(
    List<Risk> risks,
    RiskLevel overallRiskLevel,
    List<Risk> topRisks,
    double confidence
) implements ReportSection {

    public RisksSection {
        risks = risks == null ? List.of() : List.copyOf(risks);
        overallRiskLevel = overallRiskLevel == null ? RiskLevel.LOW : overallRiskLevel;
        topRisks = topRisks == null ? List.of() : List.copyOf(topRisks);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    @Override
    public String sectionName() {
        return "Risks";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.ANALYTIC;
    }

    public boolean isEmpty() {
        return risks.isEmpty();
    }

    public static RisksSection empty() {
        return new RisksSection(List.of(), RiskLevel.LOW, List.of(), 0.0);
    }
}
