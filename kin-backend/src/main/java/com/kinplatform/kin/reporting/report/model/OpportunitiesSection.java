package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.reporting.opportunity.Opportunity;

import java.util.List;

/**
 * Sección de oportunidades del reporte: reutiliza el VO {@link Opportunity} y
 * los top oportunidades ya calculados por el OpportunityEngine.
 */
public record OpportunitiesSection(
    List<Opportunity> opportunities,
    List<Opportunity> topOpportunities,
    double confidence
) implements ReportSection {

    public OpportunitiesSection {
        opportunities = opportunities == null ? List.of() : List.copyOf(opportunities);
        topOpportunities = topOpportunities == null ? List.of() : List.copyOf(topOpportunities);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    @Override
    public String sectionName() {
        return "Opportunities";
    }

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.ANALYTIC;
    }

    public boolean isEmpty() {
        return opportunities.isEmpty();
    }

    public static OpportunitiesSection empty() {
        return new OpportunitiesSection(List.of(), List.of(), 0.0);
    }
}
