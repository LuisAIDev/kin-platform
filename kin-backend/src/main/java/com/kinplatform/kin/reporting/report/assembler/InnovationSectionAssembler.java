package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.InnovationSection;

import java.util.List;

/**
 * Ensambla la sección de innovación: proyección de los valores de dimensión
 * de innovación y de las señales detectadas por la evaluación.
 */
public class InnovationSectionAssembler implements SectionAssembler<InnovationSection> {

    @Override
    public InnovationSection assemble(ReportInput input) {
        var context = input.projectContext();
        var evaluation = input.evaluation();
        return new InnovationSection(
            context.value(AnalyzedDimension.SOLUTION),
            context.value(AnalyzedDimension.VALUE_PROPOSITION),
            context.value(AnalyzedDimension.MVP),
            innovationSignals(evaluation),
            List.of(
                DimensionCoverage.of(context, AnalyzedDimension.SOLUTION),
                DimensionCoverage.of(context, AnalyzedDimension.VALUE_PROPOSITION),
                DimensionCoverage.of(context, AnalyzedDimension.MVP)));
    }

    private List<String> innovationSignals(CompletenessEvaluation evaluation) {
        return evaluation.detectedOpportunities().stream()
            .filter(signal -> signal.toLowerCase().contains("innov"))
            .toList();
    }
}
