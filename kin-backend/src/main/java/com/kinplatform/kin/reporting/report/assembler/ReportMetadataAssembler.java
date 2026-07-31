package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.ReportModel;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Ensambla la metadata del reporte: versiones, mapas de versiones de engines,
 * cobertura y confianza. {@code generatedAt} y {@code sectionsIncluded} los
 * deriva el {@code ReportBuilder} en {@code build()}.
 */
public class ReportMetadataAssembler implements SectionAssembler<ReportMetadata> {

    public static final String GENERATOR_NAME = "ReportEngine";

    private final ReportModel model;

    public ReportMetadataAssembler(ReportModel model) {
        this.model = model;
    }

    @Override
    public ReportMetadata assemble(ReportInput input) {
        var engineVersions = new LinkedHashMap<String, String>();
        engineVersions.put("ScoringEngine", input.score().engineVersion());
        engineVersions.put("RecommendationEngine", input.recommendation().engineVersion());
        engineVersions.put("RiskEngine", input.risk().engineVersion());
        engineVersions.put("OpportunityEngine", input.opportunity().engineVersion());
        engineVersions.put(GENERATOR_NAME, model.version());
        return new ReportMetadata(
            model.version(),
            model.architectureVersion(),
            null,
            GENERATOR_NAME,
            engineVersions,
            input.evaluation().coveragePercent() * 100,
            input.evaluation().confidenceScore(),
            List.of());
    }
}
