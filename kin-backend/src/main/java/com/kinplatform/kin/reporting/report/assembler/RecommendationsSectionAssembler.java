package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;

/**
 * Ensambla la sección de recomendaciones: reutiliza la lista ya ordenada y la
 * metainformación del {@code RecommendationResult}.
 */
public class RecommendationsSectionAssembler implements SectionAssembler<RecommendationsSection> {

    @Override
    public RecommendationsSection assemble(ReportInput input) {
        var result = input.recommendation();
        return new RecommendationsSection(
            result.recommendations(),
            result.priority(),
            result.confidence(),
            result.category());
    }
}
