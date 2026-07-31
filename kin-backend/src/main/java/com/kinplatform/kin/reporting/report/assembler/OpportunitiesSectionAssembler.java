package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;

/**
 * Ensambla la sección de oportunidades: reutiliza las oportunidades y los top
 * oportunidades ya calculados por el {@code OpportunityResult}.
 */
public class OpportunitiesSectionAssembler implements SectionAssembler<OpportunitiesSection> {

    @Override
    public OpportunitiesSection assemble(ReportInput input) {
        var result = input.opportunity();
        return new OpportunitiesSection(
            result.opportunities(),
            result.topOpportunities(),
            result.confidence());
    }
}
