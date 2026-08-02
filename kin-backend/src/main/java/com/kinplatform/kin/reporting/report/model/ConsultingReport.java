package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;
import java.util.UUID;

/**
 * Reporte de consultoría completo e inmutable: VO raíz que agrega las 10
 * secciones del proyecto más la sección aditiva de fuentes citadas (ADR-016).
 * Integra {@link EngineResult} para operar con la infraestructura común de
 * motores.
 *
 * <p>El id es determinista por proyecto+versión (generado por
 * {@link ReportBuilder}), de modo que un mismo proyecto con la misma versión
 * de reporte produce siempre el mismo identificador.</p>
 */
public record ConsultingReport(
    UUID id,
    UUID projectId,
    ExecutiveSummary executiveSummary,
    ScoresSection scores,
    RecommendationsSection recommendations,
    RisksSection risks,
    OpportunitiesSection opportunities,
    FinancialSection financial,
    MarketSection market,
    InnovationSection innovation,
    NextStepsSection nextSteps,
    SourcesSection sources,
    ReportMetadata metadata
) implements EngineResult {

    private static final UUID EMPTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public ConsultingReport {
        id = id == null ? EMPTY_ID : id;
        projectId = projectId == null ? EMPTY_ID : projectId;
        executiveSummary = executiveSummary == null ? ExecutiveSummary.empty() : executiveSummary;
        scores = scores == null ? ScoresSection.empty() : scores;
        recommendations = recommendations == null ? RecommendationsSection.empty() : recommendations;
        risks = risks == null ? RisksSection.empty() : risks;
        opportunities = opportunities == null ? OpportunitiesSection.empty() : opportunities;
        financial = financial == null ? FinancialSection.empty() : financial;
        market = market == null ? MarketSection.empty() : market;
        innovation = innovation == null ? InnovationSection.empty() : innovation;
        nextSteps = nextSteps == null ? NextStepsSection.empty() : nextSteps;
        sources = sources == null ? SourcesSection.empty() : sources;
        metadata = metadata == null ? ReportMetadata.empty() : metadata;
    }

    public ConsultingReport(UUID id, UUID projectId, ExecutiveSummary executiveSummary,
                            ScoresSection scores, RecommendationsSection recommendations,
                            RisksSection risks, OpportunitiesSection opportunities,
                            FinancialSection financial, MarketSection market,
                            InnovationSection innovation, NextStepsSection nextSteps,
                            ReportMetadata metadata) {
        this(id, projectId, executiveSummary, scores, recommendations, risks, opportunities,
            financial, market, innovation, nextSteps, SourcesSection.empty(), metadata);
    }

    @Override
    public double confidence() {
        return metadata.confidence();
    }

    @Override
    public String explanation() {
        return executiveSummary.summaryText();
    }

    @Override
    public String generatedBy() {
        return metadata.generatedBy();
    }

    @Override
    public String engineVersion() {
        return metadata.reportVersion();
    }

    @Override
    public boolean isEmpty() {
        return executiveSummary.isEmpty() && scores.isEmpty() && recommendations.isEmpty()
            && risks.isEmpty() && opportunities.isEmpty() && financial.isEmpty()
            && market.isEmpty() && innovation.isEmpty() && nextSteps.isEmpty()
            && sources.isEmpty();
    }

    /**
     * Retorna las secciones del reporte en el orden fijo de presentación:
     * EXECUTIVE, SCORING, ANALYTIC (recommendations, risks, opportunities),
     * PROJECTION (financial, market, innovation), AGGREGATE (nextSteps),
     * SOURCES (solo si no está vacía) y METADATA.
     */
    public List<ReportSection> sectionsInOrder() {
        var sections = new java.util.ArrayList<ReportSection>();
        sections.add(executiveSummary);
        sections.add(scores);
        sections.add(recommendations);
        sections.add(risks);
        sections.add(opportunities);
        sections.add(financial);
        sections.add(market);
        sections.add(innovation);
        sections.add(nextSteps);
        if (!sources.isEmpty()) {
            sections.add(sources);
        }
        sections.add(metadata);
        return java.util.List.copyOf(sections);
    }

    public static ConsultingReport empty() {
        return new ConsultingReport(EMPTY_ID, EMPTY_ID,
            ExecutiveSummary.empty(), ScoresSection.empty(), RecommendationsSection.empty(),
            RisksSection.empty(), OpportunitiesSection.empty(), FinancialSection.empty(),
            MarketSection.empty(), InnovationSection.empty(), NextStepsSection.empty(),
            SourcesSection.empty(), ReportMetadata.empty());
    }
}
