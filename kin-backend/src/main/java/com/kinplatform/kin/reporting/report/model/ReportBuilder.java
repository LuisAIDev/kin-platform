package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.engine.DeterministicId;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Constructor del {@link ConsultingReport} con contrato estricto.
 *
 * <p>Independiente del {@code empty()} de fallback: {@code projectId} nulo y
 * setters duplicados lanzan excepción, y {@code validate()} exige las
 * secciones obligatorias antes de construir.</p>
 */
public final class ReportBuilder {

    private final UUID projectId;
    private ExecutiveSummary executiveSummary;
    private ScoresSection scores;
    private RecommendationsSection recommendations;
    private RisksSection risks;
    private OpportunitiesSection opportunities;
    private FinancialSection financial;
    private MarketSection market;
    private InnovationSection innovation;
    private NextStepsSection nextSteps;
    private SourcesSection sources;
    private ReportMetadata metadata;

    private ReportBuilder(UUID projectId) {
        this.projectId = projectId;
    }

    public static ReportBuilder create(UUID projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser nulo");
        }
        return new ReportBuilder(projectId);
    }

    public ReportBuilder executiveSummary(ExecutiveSummary section) {
        checkNotAssigned(executiveSummary, "executiveSummary");
        executiveSummary = section;
        return this;
    }

    public ReportBuilder scores(ScoresSection section) {
        checkNotAssigned(scores, "scores");
        scores = section;
        return this;
    }

    public ReportBuilder recommendations(RecommendationsSection section) {
        checkNotAssigned(recommendations, "recommendations");
        recommendations = section;
        return this;
    }

    public ReportBuilder risks(RisksSection section) {
        checkNotAssigned(risks, "risks");
        risks = section;
        return this;
    }

    public ReportBuilder opportunities(OpportunitiesSection section) {
        checkNotAssigned(opportunities, "opportunities");
        opportunities = section;
        return this;
    }

    public ReportBuilder financial(FinancialSection section) {
        checkNotAssigned(financial, "financial");
        financial = section;
        return this;
    }

    public ReportBuilder market(MarketSection section) {
        checkNotAssigned(market, "market");
        market = section;
        return this;
    }

    public ReportBuilder innovation(InnovationSection section) {
        checkNotAssigned(innovation, "innovation");
        innovation = section;
        return this;
    }

    public ReportBuilder nextSteps(NextStepsSection section) {
        checkNotAssigned(nextSteps, "nextSteps");
        nextSteps = section;
        return this;
    }

    public ReportBuilder sources(SourcesSection section) {
        checkNotAssigned(sources, "sources");
        sources = section;
        return this;
    }

    public ReportBuilder metadata(ReportMetadata section) {
        checkNotAssigned(metadata, "metadata");
        metadata = section;
        return this;
    }

    public void validate() {
        if (executiveSummary == null) {
            throw new IllegalStateException("executiveSummary es obligatoria");
        }
        if (scores == null) {
            throw new IllegalStateException("scores es obligatoria");
        }
        if (metadata == null) {
            throw new IllegalStateException("metadata es obligatoria");
        }
    }

    public ConsultingReport build() {
        validate();
        var now = OffsetDateTime.now();
        var id = DeterministicId.from(projectId.toString(), "ConsultingReport",
            metadata.reportVersion());
        var finalMetadata = metadata.withGeneratedAt(now).withSectionsIncluded(sectionsIncluded());
        return new ConsultingReport(id, projectId, executiveSummary, scores, recommendations,
            risks, opportunities, financial, market, innovation, nextSteps, sources, finalMetadata);
    }

    private void checkNotAssigned(Object section, String name) {
        if (section != null) {
            throw new IllegalStateException(name + " ya fue asignada");
        }
    }

    private List<String> sectionsIncluded() {
        var names = new ArrayList<String>();
        if (executiveSummary != null) names.add(executiveSummary.sectionName());
        if (scores != null) names.add(scores.sectionName());
        if (recommendations != null) names.add(recommendations.sectionName());
        if (risks != null) names.add(risks.sectionName());
        if (opportunities != null) names.add(opportunities.sectionName());
        if (financial != null) names.add(financial.sectionName());
        if (market != null) names.add(market.sectionName());
        if (innovation != null) names.add(innovation.sectionName());
        if (nextSteps != null) names.add(nextSteps.sectionName());
        if (sources != null && !sources.isEmpty()) names.add(sources.sectionName());
        names.add(metadata.sectionName());
        return List.copyOf(names);
    }
}
