package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;
import java.util.UUID;

/**
 * Reporte de consultoría completo e inmutable: VO raíz que agrega las 10
 * secciones del proyecto. Integra {@link EngineResult} para operar con la
 * infraestructura común de motores.
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
        metadata = metadata == null ? ReportMetadata.empty() : metadata;
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
            && market.isEmpty() && innovation.isEmpty() && nextSteps.isEmpty();
    }

    /**
     * Retorna las 10 secciones del reporte en el orden fijo de presentación:
     * EXECUTIVE, SCORING, ANALYTIC (recommendations, risks, opportunities),
     * PROJECTION (financial, market, innovation), AGGREGATE (nextSteps), METADATA.
     */
    public List<ReportSection> sectionsInOrder() {
        return List.of(
            executiveSummary,
            scores,
            recommendations,
            risks,
            opportunities,
            financial,
            market,
            innovation,
            nextSteps,
            metadata
        );
    }

    public static ConsultingReport empty() {
        return new ConsultingReport(EMPTY_ID, EMPTY_ID,
            ExecutiveSummary.empty(), ScoresSection.empty(), RecommendationsSection.empty(),
            RisksSection.empty(), OpportunitiesSection.empty(), FinancialSection.empty(),
            MarketSection.empty(), InnovationSection.empty(), NextStepsSection.empty(),
            ReportMetadata.empty());
    }
}
