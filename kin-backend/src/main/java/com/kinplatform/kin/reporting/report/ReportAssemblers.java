package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.reporting.report.assembler.ExecutiveSummaryAssembler;
import com.kinplatform.kin.reporting.report.assembler.FinancialSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.InnovationSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.MarketSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.NextStepsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.OpportunitiesSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.RecommendationsSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ReportMetadataAssembler;
import com.kinplatform.kin.reporting.report.assembler.RisksSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.ScoresSectionAssembler;
import com.kinplatform.kin.reporting.report.assembler.SourcesSectionAssembler;

/**
 * Agrupación tipada de los 11 {@link SectionAssembler} del reporte.
 *
 * <p>Evita el auto-descubrimiento con {@code List<?>} + casts y el dispatch
 * por {@code switch}: el {@code ReportEngine} accede a cada ensamblador por su
 * campo tipado.</p>
 */
public record ReportAssemblers(
    ExecutiveSummaryAssembler executiveSummary,
    ScoresSectionAssembler scores,
    RecommendationsSectionAssembler recommendations,
    RisksSectionAssembler risks,
    OpportunitiesSectionAssembler opportunities,
    FinancialSectionAssembler financial,
    MarketSectionAssembler market,
    InnovationSectionAssembler innovation,
    NextStepsSectionAssembler nextSteps,
    ReportMetadataAssembler metadata,
    SourcesSectionAssembler sources
) {

    public ReportAssemblers(ExecutiveSummaryAssembler executiveSummary,
                            ScoresSectionAssembler scores,
                            RecommendationsSectionAssembler recommendations,
                            RisksSectionAssembler risks,
                            OpportunitiesSectionAssembler opportunities,
                            FinancialSectionAssembler financial,
                            MarketSectionAssembler market,
                            InnovationSectionAssembler innovation,
                            NextStepsSectionAssembler nextSteps,
                            ReportMetadataAssembler metadata) {
        this(executiveSummary, scores, recommendations, risks, opportunities, financial,
            market, innovation, nextSteps, metadata, new SourcesSectionAssembler());
    }
}
