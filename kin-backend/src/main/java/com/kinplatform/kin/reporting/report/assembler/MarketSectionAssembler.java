package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.MarketSection;

import java.util.List;

/**
 * Ensambla la sección de mercado: proyección directa de los valores de
 * dimensión de mercado ya presentes en el contexto.
 */
public class MarketSectionAssembler implements SectionAssembler<MarketSection> {

    @Override
    public MarketSection assemble(ReportInput input) {
        var context = input.projectContext();
        return new MarketSection(
            context.value(AnalyzedDimension.SECTOR),
            context.value(AnalyzedDimension.TARGET_CUSTOMER),
            context.value(AnalyzedDimension.CITY),
            context.value(AnalyzedDimension.PROBLEM),
            List.of(
                DimensionCoverage.of(context, AnalyzedDimension.SECTOR),
                DimensionCoverage.of(context, AnalyzedDimension.TARGET_CUSTOMER),
                DimensionCoverage.of(context, AnalyzedDimension.CITY),
                DimensionCoverage.of(context, AnalyzedDimension.PROBLEM)));
    }
}
