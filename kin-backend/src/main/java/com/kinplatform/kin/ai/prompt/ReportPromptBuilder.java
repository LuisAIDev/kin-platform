package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.ReportSection;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter;
import com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter;

import java.util.List;

/**
 * Construye el prompt para la fase de reporte (explicación del ConsultingReport).
 *
 * <p>Itera {@link ConsultingReport#sectionsInOrder()} en orden fijo y usa
 * {@link SectionFormatter} por sección (inyectados ordenados por {@link ReportSectionKind}).
 *
 * <p>Añade instrucción final fija: el LLM solo explica, no decide.
 */
public class ReportPromptBuilder {

    private final ExecutiveSummaryFormatter executiveSummaryFormatter;
    private final ScoresSectionFormatter scoresSectionFormatter;
    private final RecommendationsSectionFormatter recommendationsSectionFormatter;
    private final RisksSectionFormatter risksSectionFormatter;
    private final OpportunitiesSectionFormatter opportunitiesSectionFormatter;
    private final FinancialSectionFormatter financialSectionFormatter;
    private final MarketSectionFormatter marketSectionFormatter;
    private final InnovationSectionFormatter innovationSectionFormatter;
    private final NextStepsSectionFormatter nextStepsSectionFormatter;
    private final ReportMetadataFormatter reportMetadataFormatter;

    public ReportPromptBuilder(List<SectionFormatter<?>> formatters) {
        if (formatters == null || formatters.isEmpty()) {
            throw new IllegalArgumentException("Se requieren al menos un SectionFormatter");
        }
        this.executiveSummaryFormatter = requireSingle(formatters, ExecutiveSummaryFormatter.class);
        this.scoresSectionFormatter = requireSingle(formatters, ScoresSectionFormatter.class);
        this.recommendationsSectionFormatter = requireSingle(formatters, RecommendationsSectionFormatter.class);
        this.risksSectionFormatter = requireSingle(formatters, RisksSectionFormatter.class);
        this.opportunitiesSectionFormatter = requireSingle(formatters, OpportunitiesSectionFormatter.class);
        this.financialSectionFormatter = requireSingle(formatters, FinancialSectionFormatter.class);
        this.marketSectionFormatter = requireSingle(formatters, MarketSectionFormatter.class);
        this.innovationSectionFormatter = requireSingle(formatters, InnovationSectionFormatter.class);
        this.nextStepsSectionFormatter = requireSingle(formatters, NextStepsSectionFormatter.class);
        this.reportMetadataFormatter = requireSingle(formatters, ReportMetadataFormatter.class);
    }

    public String build(PromptRequest request) {
        if (request.type() != com.kinplatform.kin.ai.PromptType.REPORT) {
            throw new IllegalArgumentException("ReportPromptBuilder solo soporta REPORT");
        }

        ConsultingReport report = request.consultingReport();
        if (report == null) {
            throw new IllegalArgumentException("consultingReport no puede ser null para REPORT");
        }

        var sb = new StringBuilder();
        sb.append("=== CONSULTING REPORT ===\n");
        sb.append("Project: ").append(report.projectId()).append("\n");
        sb.append("Generated: ").append(report.metadata().generatedAt()).append("\n");
        sb.append("Version: ").append(report.metadata().reportVersion()).append("\n\n");

        List<ReportSection> sections = report.sectionsInOrder();
        for (ReportSection section : sections) {
            sb.append(format(section)).append("\n\n");
        }

        sb.append("--- INSTRUCCIÓN PARA EL LLM ---\n");
        sb.append("""
            Eres KIN. Explica el reporte anterior de forma natural, profesional y conversacional en español.
            No añadas secciones nuevas. No recalcules scores. No opines sobre viabilidad.
            Usa los datos tal cual están.
            """);

        return sb.toString();
    }

    private String format(ReportSection section) {
        if (section instanceof ExecutiveSummary s) {
            return executiveSummaryFormatter.format(s);
        }
        if (section instanceof ScoresSection s) {
            return scoresSectionFormatter.format(s);
        }
        if (section instanceof RecommendationsSection s) {
            return recommendationsSectionFormatter.format(s);
        }
        if (section instanceof RisksSection s) {
            return risksSectionFormatter.format(s);
        }
        if (section instanceof OpportunitiesSection s) {
            return opportunitiesSectionFormatter.format(s);
        }
        if (section instanceof FinancialSection s) {
            return financialSectionFormatter.format(s);
        }
        if (section instanceof MarketSection s) {
            return marketSectionFormatter.format(s);
        }
        if (section instanceof InnovationSection s) {
            return innovationSectionFormatter.format(s);
        }
        if (section instanceof NextStepsSection s) {
            return nextStepsSectionFormatter.format(s);
        }
        if (section instanceof ReportMetadata s) {
            return reportMetadataFormatter.format(s);
        }
        throw new IllegalArgumentException("No existe formatter para sección " + section.getClass().getSimpleName());
    }

    private static <T extends SectionFormatter<?>> T requireSingle(List<SectionFormatter<?>> formatters, Class<T> type) {
        List<T> matches = formatters.stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Se requiere exactamente un " + type.getSimpleName());
        }
        return matches.get(0);
    }
}
