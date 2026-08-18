package com.kinplatform.kin.export.template;

import com.kinplatform.kin.export.ExportInput;
import com.kinplatform.kin.export.ProjectExportAssembler;
import com.kinplatform.kin.export.StructuredInfo;
import com.kinplatform.kin.export.model.ExportBlock;
import com.kinplatform.kin.export.model.ExportBlockType;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportSection;
import com.kinplatform.kin.export.model.ExportTable;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ReportSection;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mapea la estructura de una {@link ExportTemplate} con los datos reales del
 * proyecto KIN ({@link ExportInput}).
 *
 * <p>El mapeo es estructural (no una coincidencia exacta de strings): cada
 * sección de la plantilla se clasifica por palabras clave normalizadas y se
 * rellena con los bloques de KIN correspondientes (reporte, project_info o
 * metadatos del proyecto). Si no hay datos equivalentes, la sección se conserva
 * con el marcador "Pendiente de información". Nunca se inventa contenido.</p>
 */
public final class ProjectExportTemplateMapper {

    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\d+(\\s+\\d+)*\\s*");
    private static final Pattern LEADING_PART = Pattern.compile("^(capitulo|seccion|parte|anexo)\\s+\\d+\\s*");

    private static final String PENDIENTE = "Pendiente de información";

    private final ProjectExportAssembler assembler;

    public ProjectExportTemplateMapper(ProjectExportAssembler assembler) {
        this.assembler = assembler;
    }

    public ExportDocument map(ExportTemplate template, ExportInput input) {
        if (template == null) {
            throw new IllegalArgumentException("ExportTemplate no puede ser null");
        }
        List<ExportSection> sections = new ArrayList<>();
        for (TemplateSection section : template.sections()) {
            sections.add(mapSection(section, input));
        }
        String subtitle = input.categoryName().isBlank() ? "Proyecto KIN" : "Proyecto KIN — " + input.categoryName();
        return new ExportDocument(
                input.title().isBlank() ? "Proyecto" : input.title(),
                subtitle,
                java.time.OffsetDateTime.now(),
                sections);
    }

    private ExportSection mapSection(TemplateSection section, ExportInput input) {
        String key = normalize(section.title());
        List<ExportBlock> blocks = blocksFor(key, section, input);
        return ExportSection.of(section.title(), blocks);
    }

    private List<ExportBlock> blocksFor(String key, TemplateSection section, ExportInput input) {
        if (isAny(
                key,
                "identificacion",
                "datos del proyecto",
                "datos generales",
                "informacion del proyecto",
                "informacion general",
                "nombre del proyecto",
                "portada")) {
            return withoutTitle(assembler.identificationBlocks(input));
        }
        if (isAny(key, "introduccion", "presentacion", "prologo", "objeto")) {
            return introBlocks(input);
        }
        if (isAny(key, "resumen ejecutivo", "resumen general", "resumen", "executive summary", "summary")) {
            return reportBlocks(input, r -> r.executiveSummary());
        }
        if (isAny(key, "metricas", "viabilidad", "scoring", "score", "metricas de viabilidad")) {
            return reportBlocks(input, r -> r.scores());
        }
        if (isAny(key, "recomendaciones")) {
            return reportBlocks(input, r -> r.recommendations());
        }
        if (isAny(key, "riesgos")) {
            return reportBlocks(input, r -> r.risks());
        }
        if (isAny(key, "oportunidades")) {
            return reportBlocks(input, r -> r.opportunities());
        }
        if (isAny(key, "financier", "presupuesto", "costos", "finanzas", "finan")) {
            return financialBlocks(input);
        }
        if (isAny(key, "mercado", "analisis de mercado", "estudio de mercado", "investigacion de mercado")) {
            return marketBlocks(input);
        }
        if (isAny(key, "innovacion")) {
            return reportBlocks(input, r -> r.innovation());
        }
        if (isAny(key, "proximos pasos", "siguientes pasos", "conclusiones", "conclusion", "resultados")) {
            return reportBlocks(input, r -> r.nextSteps());
        }
        if (isAny(key, "fuentes", "referencias", "bibliografia", "fuentes citadas")) {
            return reportBlocks(input, r -> r.sources());
        }
        List<ExportBlock> infoBlocks = infoTableForKey(input, key);
        if (infoBlocks != null) {
            return infoBlocks;
        }
        if (isAny(key, "objetivos", "objetivo", "justificacion", "metodologia", "cronograma")) {
            List<ExportBlock> fromInfo = infoSubstringBlocks(input, key);
            if (fromInfo != null) {
                return fromInfo;
            }
            return List.of(ExportBlock.highlight(PENDIENTE));
        }
        return List.of(ExportBlock.highlight(PENDIENTE));
    }

    private List<ExportBlock> introBlocks(ExportInput input) {
        List<ExportBlock> blocks = new ArrayList<>();
        if (!input.description().isBlank()) {
            blocks.add(ExportBlock.paragraph(input.description()));
        }
        blocks.addAll(reportBlocks(input, r -> r.executiveSummary()));
        if (blocks.isEmpty()) {
            blocks.add(ExportBlock.highlight(PENDIENTE));
        }
        return blocks;
    }

    private List<ExportBlock> financialBlocks(ExportInput input) {
        List<ExportBlock> blocks = new ArrayList<>(reportBlocks(input, r -> r.financial()));
        List<ExportBlock> info = infoTableForSection(input, "FINANZAS");
        if (info != null) {
            blocks.addAll(info);
        }
        if (blocks.isEmpty()) {
            blocks.add(ExportBlock.highlight(PENDIENTE));
        }
        return blocks;
    }

    private List<ExportBlock> marketBlocks(ExportInput input) {
        List<ExportBlock> blocks = new ArrayList<>(reportBlocks(input, r -> r.market()));
        List<ExportBlock> info = infoTableForSection(input, "MERCADO");
        if (info != null) {
            blocks.addAll(info);
        }
        if (blocks.isEmpty()) {
            blocks.add(ExportBlock.highlight(PENDIENTE));
        }
        return blocks;
    }

    private List<ExportBlock> reportBlocks(ExportInput input, SectionAccessor accessor) {
        ConsultingReport report = input.report();
        if (report == null) {
            return List.of(ExportBlock.highlight(PENDIENTE));
        }
        ReportSection section = accessor.of(report);
        List<ExportBlock> blocks = assembler.blocksFor(section);
        if (blocks.isEmpty()) {
            return List.of(ExportBlock.highlight(PENDIENTE));
        }
        return blocks;
    }

    private List<ExportBlock> infoTableForSection(ExportInput input, String sectionName) {
        Map<String, List<StructuredInfo>> groups = groupBySection(input);
        List<StructuredInfo> entries = groups.get(normalize(sectionName));
        return entries == null || entries.isEmpty() ? null : tableBlocks(entries);
    }

    private List<ExportBlock> infoTableForKey(ExportInput input, String key) {
        Map<String, List<StructuredInfo>> groups = groupBySection(input);
        List<StructuredInfo> entries = groups.get(key);
        return entries == null || entries.isEmpty() ? null : tableBlocks(entries);
    }

    private List<ExportBlock> infoSubstringBlocks(ExportInput input, String key) {
        List<StructuredInfo> matches = input.info().stream()
                .filter(e -> normalize(e.section()).contains(key)
                        || normalize(e.key()).contains(key))
                .toList();
        return matches.isEmpty() ? null : tableBlocks(matches);
    }

    private List<ExportBlock> tableBlocks(List<StructuredInfo> entries) {
        List<String> header = List.of("Campo", "Valor", "Fuente");
        List<List<String>> rows = new ArrayList<>();
        for (StructuredInfo entry : entries) {
            rows.add(List.of(entry.key(), entry.value(), entry.sourceType()));
        }
        return List.of(ExportBlock.table(ExportTable.of(header, rows)));
    }

    private Map<String, List<StructuredInfo>> groupBySection(ExportInput input) {
        Map<String, List<StructuredInfo>> groups = new LinkedHashMap<>();
        for (StructuredInfo entry : input.info()) {
            groups.computeIfAbsent(normalize(entry.section()), k -> new ArrayList<>())
                    .add(entry);
        }
        return groups;
    }

    private List<ExportBlock> withoutTitle(List<ExportBlock> blocks) {
        return blocks.stream().filter(b -> b.type() != ExportBlockType.TITLE).toList();
    }

    private boolean isAny(String key, String... words) {
        for (String word : words) {
            if (key.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Normaliza un título de sección: minúsculas, sin acentos, sin numeración. */
    static String normalize(String title) {
        if (title == null) {
            return "";
        }
        String text = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        text = LEADING_NUMBER.matcher(text).replaceFirst("").trim();
        text = LEADING_PART.matcher(text).replaceFirst("").trim();
        return text;
    }

    private interface SectionAccessor {
        ReportSection of(ConsultingReport report);
    }
}
