package com.kinplatform.kin.export;

import com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter;
import com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.SourcesSectionFormatter;
import com.kinplatform.kin.export.assemble.MarkdownTextToBlocks;
import com.kinplatform.kin.export.model.ExportBlock;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportMode;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.kin.export.model.ExportTable;
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
import com.kinplatform.kin.reporting.report.model.SourcesSection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ensamblador del documento exportado del proyecto (dominio puro, sin Spring).
 *
 * <p>Construye un {@link ExportDocument} a partir de {@link ExportInput}
 * (Project + ConsultingReport + project_info). El historial de chat NO es
 * fuente de datos. Regla de contenido: no se inventa información; las secciones
 * sin datos se omiten o se marcan como pendientes.</p>
 */
public class ProjectExportAssembler {

    private final ExecutiveSummaryFormatter executiveSummaryFormatter;
    private final ScoresSectionFormatter scoresSectionFormatter;
    private final RecommendationsSectionFormatter recommendationsSectionFormatter;
    private final RisksSectionFormatter risksSectionFormatter;
    private final OpportunitiesSectionFormatter opportunitiesSectionFormatter;
    private final FinancialSectionFormatter financialSectionFormatter;
    private final MarketSectionFormatter marketSectionFormatter;
    private final InnovationSectionFormatter innovationSectionFormatter;
    private final NextStepsSectionFormatter nextStepsSectionFormatter;
    private final SourcesSectionFormatter sourcesSectionFormatter;

    public ProjectExportAssembler() {
        this.executiveSummaryFormatter = new ExecutiveSummaryFormatter();
        this.scoresSectionFormatter = new ScoresSectionFormatter();
        this.recommendationsSectionFormatter = new RecommendationsSectionFormatter();
        this.risksSectionFormatter = new RisksSectionFormatter();
        this.opportunitiesSectionFormatter = new OpportunitiesSectionFormatter();
        this.financialSectionFormatter = new FinancialSectionFormatter();
        this.marketSectionFormatter = new MarketSectionFormatter();
        this.innovationSectionFormatter = new InnovationSectionFormatter();
        this.nextStepsSectionFormatter = new NextStepsSectionFormatter();
        this.sourcesSectionFormatter = new SourcesSectionFormatter();
    }

    public ExportDocument assemble(ExportInput input) {
        if (input == null) {
            throw new IllegalArgumentException("ExportInput no puede ser null");
        }
        List<ExportSection> sections = new ArrayList<>();
        sections.add(identificationSection(input));

        if (input.mode() == ExportMode.SUMMARY) {
            if (input.report() != null) {
                sections.addAll(summarySections(input.report()));
            }
        } else {
            if (input.report() != null) {
                sections.addAll(reportSections(input.report()));
            }
            sections.addAll(infoSections(input.info()));
        }

        String subtitle = "Proyecto KIN";
        if (!input.categoryName().isBlank()) {
            subtitle = "Proyecto KIN — " + input.categoryName();
        }
        return new ExportDocument(
                input.title().isBlank() ? "Proyecto" : input.title(),
                subtitle,
                java.time.OffsetDateTime.now(),
                sections);
    }

    private ExportSection identificationSection(ExportInput input) {
        return ExportSection.of("Identificación del proyecto", identificationBlocks(input));
    }

    /**
     * Bloques de identificación del proyecto (título, descripción, metadatos,
     * resumen de KIN). Compartido con el modo plantilla para no duplicar lógica.
     */
    public List<ExportBlock> identificationBlocks(ExportInput input) {
        List<ExportBlock> blocks = new ArrayList<>();
        blocks.add(ExportBlock.title(input.title().isBlank() ? "Proyecto" : input.title()));

        if (!input.description().isBlank()) {
            blocks.add(ExportBlock.subtitle("Descripción"));
            blocks.add(ExportBlock.paragraph(input.description()));
        }

        List<String> meta = new ArrayList<>();
        if (!input.categoryName().isBlank()) {
            meta.add("**Categoría:** " + input.categoryName());
        }
        if (!input.status().isBlank()) {
            meta.add("**Estado:** " + input.status());
        }
        if (input.viabilityScore() != null) {
            meta.add("**Score de viabilidad:** " + input.viabilityScore() + " / 100");
        }
        if (input.createdAt() != null) {
            meta.add("**Creado:** " + input.createdAt().toLocalDate().toString());
        }
        if (input.updatedAt() != null) {
            meta.add("**Actualizado:** " + input.updatedAt().toLocalDate().toString());
        }
        if (!meta.isEmpty()) {
            blocks.add(ExportBlock.list(meta));
        }

        if (input.aiSummary() != null && !input.aiSummary().isBlank()) {
            blocks.add(ExportBlock.subtitle("Resumen de KIN"));
            blocks.add(ExportBlock.paragraph(input.aiSummary()));
        }

        if (input.description().isBlank()
                && meta.isEmpty()
                && (input.aiSummary() == null || input.aiSummary().isBlank())) {
            blocks.add(ExportBlock.highlight("Pendiente de información"));
        }
        return blocks;
    }

    private List<ExportSection> summarySections(ConsultingReport report) {
        List<ExportSection> sections = new ArrayList<>();
        if (!report.executiveSummary().isEmpty()) {
            addIfPresent(sections, "Resumen Ejecutivo", report.executiveSummary());
        }
        if (!report.scores().isEmpty()) {
            addIfPresent(sections, "Métricas de Viabilidad", report.scores());
        }
        return sections;
    }

    private List<ExportSection> reportSections(ConsultingReport report) {
        List<ExportSection> sections = new ArrayList<>();
        for (ReportSection section : report.sectionsInOrder()) {
            if (section instanceof ReportMetadata) {
                continue;
            }
            addIfPresent(sections, section.sectionName(), section);
        }
        return sections;
    }

    private void addIfPresent(List<ExportSection> sections, String fallbackTitle, ReportSection section) {
        ExportSection built = toSection(fallbackTitle, section);
        if (built != null) {
            sections.add(built);
        }
    }

    private ExportSection toSection(String fallbackTitle, ReportSection section) {
        List<ExportBlock> blocks = blocksFor(section);
        if (blocks.isEmpty()) {
            return null;
        }
        return ExportSection.of(sectionTitleFor(section, fallbackTitle), blocks);
    }

    /**
     * Bloques de una sección del reporte (vía SectionFormatter). Compartido con
     * el modo plantilla para no duplicar la lógica de formato.
     */
    public List<ExportBlock> blocksFor(ReportSection section) {
        return MarkdownTextToBlocks.convert(format(section)).blocks();
    }

    /** Título legible de la sección (extraído del formatter) o el fallback. */
    public String sectionTitleFor(ReportSection section, String fallbackTitle) {
        String title = MarkdownTextToBlocks.convert(format(section)).title();
        return title == null || title.isBlank() ? fallbackTitle : title;
    }

    private List<ExportSection> infoSections(List<StructuredInfo> info) {
        Map<String, List<StructuredInfo>> bySection = new LinkedHashMap<>();
        for (StructuredInfo entry : info) {
            bySection.computeIfAbsent(entry.section(), k -> new ArrayList<>()).add(entry);
        }
        List<ExportSection> sections = new ArrayList<>();
        for (Map.Entry<String, List<StructuredInfo>> group : bySection.entrySet()) {
            List<ExportBlock> blocks = new ArrayList<>();
            List<String> header = List.of("Campo", "Valor", "Fuente");
            List<List<String>> rows = new ArrayList<>();
            for (StructuredInfo entry : group.getValue()) {
                rows.add(List.of(entry.key(), entry.value(), entry.sourceType()));
            }
            if (!rows.isEmpty()) {
                blocks.add(ExportBlock.table(ExportTable.of(header, rows)));
                sections.add(ExportSection.of("Información: " + group.getKey(), blocks));
            }
        }
        return sections;
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
        if (section instanceof SourcesSection s) {
            return sourcesSectionFormatter.format(s);
        }
        throw new IllegalArgumentException(
                "No existe formatter para sección " + section.getClass().getSimpleName());
    }
}
