package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.ai.prompt.SectionFormatter;
import com.kinplatform.kin.reporting.report.model.NextStep;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;

import java.util.List;

/**
 * Formatea {@link NextStepsSection} a Markdown ligero.
 */
public class NextStepsSectionFormatter implements SectionFormatter<NextStepsSection> {

    @Override
    public ReportSectionKind kind() {
        return ReportSectionKind.AGGREGATE;
    }

    @Override
    public String format(NextStepsSection section) {
        var sb = new StringBuilder();
        sb.append("## Próximos Pasos\n\n");

        List<NextStep> steps = section.nextSteps();
        if (steps.isEmpty()) {
            sb.append("_Sin próximos pasos definidos._\n");
            return sb.toString();
        }

        for (int i = 0; i < steps.size(); i++) {
            var step = steps.get(i);
            sb.append("### ").append(i + 1).append(". ").append(step.title()).append("\n\n");
            sb.append("**Origen:** ").append(step.source()).append("  \n");
            sb.append("**Prioridad:** ").append(step.priority()).append("/10  \n");
            sb.append("**Razón:** ").append(step.reason()).append("\n\n");
        }

        sb.append("**Total:** ").append(steps.size()).append(" pasos siguientes\n");
        return sb.toString();
    }
}