package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.FinancialSection;

import java.util.List;

/**
 * Ensambla la sección financiera: proyección directa de los valores de
 * dimensión financiera ya presentes en el contexto, sin estimaciones.
 */
public class FinancialSectionAssembler implements SectionAssembler<FinancialSection> {

    @Override
    public FinancialSection assemble(ReportInput input) {
        var context = input.projectContext();
        return new FinancialSection(
            context.value(AnalyzedDimension.REVENUE_MODEL),
            context.value(AnalyzedDimension.RESOURCES),
            context.value(AnalyzedDimension.OBJECTIVES),
            List.of(
                DimensionCoverage.of(context, AnalyzedDimension.REVENUE_MODEL),
                DimensionCoverage.of(context, AnalyzedDimension.RESOURCES),
                DimensionCoverage.of(context, AnalyzedDimension.OBJECTIVES)));
    }
}
