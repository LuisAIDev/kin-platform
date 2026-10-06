package com.kinplatform.platform.reporting.report.assembler;

import com.kinplatform.platform.reporting.report.ReportInput;
import com.kinplatform.platform.reporting.report.SectionAssembler;
import com.kinplatform.platform.reporting.report.model.RisksSection;

/**
 * Ensambla la sección de riesgos: reutiliza los riesgos, los top riesgos y el
 * nivel global ya calculados por el {@code RiskResult}.
 */
public class RisksSectionAssembler implements SectionAssembler<RisksSection> {

    @Override
    public RisksSection assemble(ReportInput input) {
        var result = input.risk();
        return new RisksSection(
            result.risks(),
            result.overallRiskLevel(),
            result.topRisks(),
            result.confidence());
    }
}


