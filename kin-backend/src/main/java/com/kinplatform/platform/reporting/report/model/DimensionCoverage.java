package com.kinplatform.platform.reporting.report.model;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;

/**
 * Cobertura de una dimensión del proyecto: indica si el valor está presente
 * en el contexto (proyección directa de {@code isDimensionCovered}).
 */
public record DimensionCoverage(
    AnalyzedDimension dimension,
    boolean covered
) {

    public static DimensionCoverage of(ProjectContext context, AnalyzedDimension dimension) {
        return new DimensionCoverage(dimension, context.isDimensionCovered(dimension));
    }
}


