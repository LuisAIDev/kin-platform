package com.kinplatform.kin.export.application;

import com.kinplatform.kin.export.ExportInput;
import com.kinplatform.kin.export.ProjectExportAssembler;
import com.kinplatform.kin.export.StructuredInfo;
import com.kinplatform.kin.export.model.ExportDocument;
import com.kinplatform.kin.export.model.ExportFormat;
import com.kinplatform.kin.export.model.ExportMode;
import com.kinplatform.kin.export.renderer.ExportRendererFactory;
import com.kinplatform.kin.export.template.ExportTemplate;
import com.kinplatform.kin.export.template.ProjectExportTemplateMapper;
import com.kinplatform.kin.export.template.ProjectExportTemplateParser;
import com.kinplatform.kin.reporting.report.ReportRepository;
import com.kinplatform.kin.reporting.report.model.ReportSection;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.projectdoc.ProjectDocument;
import com.kinplatform.projectdoc.ProjectDocumentService;
import com.kinplatform.projectdoc.ProjectDocumentStatus;
import com.kinplatform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.projectinfo.ProjectStructuredInfoRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementación de {@link ProjectExportService}.
 *
 * <p>Coordina la carga de datos reales del proyecto (Project + ConsultingReport
 * + project_info), el ensamblado del modelo neutral y el renderizado al formato
 * solicitado. El historial de chat NO interviene como fuente. Verifica la
 * propiedad del proyecto y, cuando se usa plantilla, la del documento de
 * referencia (404 si no existe o es ajeno, sin filtrar existencia).</p>
 */
@Service
public class ProjectExportServiceImpl implements ProjectExportService {

    private final ProjectRepository projectRepository;
    private final ReportRepository reportRepository;
    private final ProjectStructuredInfoRepository structuredInfoRepository;
    private final ProjectDocumentService documentService;
    private final ProjectExportAssembler assembler;
    private final ProjectExportTemplateParser templateParser;
    private final ProjectExportTemplateMapper templateMapper;
    private final ExportRendererFactory rendererFactory;

    public ProjectExportServiceImpl(
            ProjectRepository projectRepository,
            ReportRepository reportRepository,
            ProjectStructuredInfoRepository structuredInfoRepository,
            ProjectDocumentService documentService) {
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.structuredInfoRepository = structuredInfoRepository;
        this.documentService = documentService;
        this.assembler = new ProjectExportAssembler();
        this.templateParser = new ProjectExportTemplateParser();
        this.templateMapper = new ProjectExportTemplateMapper(this.assembler);
        this.rendererFactory = new ExportRendererFactory();
    }

    @Override
    public ExportResult export(
            UUID userId, UUID projectId, ExportFormat format, ExportMode mode, UUID templateDocumentId) {
        if (format == null) {
            throw new IllegalArgumentException("formato de exportación no puede ser null");
        }
        Project project = requireOwned(userId, projectId);
        ExportDocument document = buildDocument(userId, project, mode, templateDocumentId);
        byte[] bytes = rendererFactory.rendererFor(format).render(document);
        return new ExportResult(format, bytes, filename(project.getTitle(), format));
    }

    private ExportDocument buildDocument(UUID userId, Project project, ExportMode mode, UUID templateDocumentId) {
        ExportInput input = toInput(project, mode);
        if (templateDocumentId == null) {
            return assembler.assemble(input);
        }
        ProjectDocument reference = requireTemplateDocument(userId, project.getId(), templateDocumentId);
        ExportTemplate template =
                templateParser.parse(reference.getFilename(), reference.getMimeType(), reference.getExtractedText());
        return templateMapper.map(template, input);
    }

    private ProjectDocument requireTemplateDocument(UUID userId, UUID projectId, UUID documentId) {
        ProjectDocument document = documentService
                .findOwned(userId, projectId, documentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento de referencia no encontrado"));
        if (document.getStatus() != ProjectDocumentStatus.PROCESADO
                || document.getExtractedText() == null
                || document.getExtractedText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El documento seleccionado no puede utilizarse como plantilla.");
        }
        return document;
    }

    @Override
    public ExportOptions options(UUID userId, UUID projectId) {
        Project project = requireOwned(userId, projectId);
        var storedReport = reportRepository.findLatest(projectId);
        boolean hasReport = storedReport.isPresent();
        List<String> reportSections = storedReport
                .map(s -> s.report().sectionsInOrder().stream()
                        .map(ReportSection::sectionName)
                        .filter(name -> !"ReportMetadata".equals(name))
                        .toList())
                .orElse(List.of());
        List<String> infoSections = structuredInfoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId).stream()
                .map(ProjectStructuredInfo::getSection)
                .distinct()
                .sorted()
                .toList();
        return new ExportOptions(
                List.of(ExportFormat.DOCX.name(), ExportFormat.PDF.name(), ExportFormat.MARKDOWN.name()),
                hasReport,
                reportSections,
                infoSections,
                filenameBase(project.getTitle()));
    }

    private ExportInput toInput(Project project, ExportMode mode) {
        var report = reportRepository
                .findLatest(project.getId())
                .map(ReportRepository.StoredReport::report)
                .orElse(null);
        List<StructuredInfo> info =
                structuredInfoRepository.findByProjectIdOrderBySectionAscKeyAsc(project.getId()).stream()
                        .map(e -> new StructuredInfo(
                                e.getSection(),
                                e.getKey(),
                                e.getValue(),
                                e.getSourceType().name()))
                        .toList();
        return new ExportInput(
                project.getTitle(),
                project.getDescription(),
                project.getCategory() == null ? "" : project.getCategory().getName(),
                project.getStatus() == null ? "" : project.getStatus().name(),
                project.getViabilityScore(),
                project.getAiSummary(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                report,
                info,
                mode);
    }

    private Project requireOwned(UUID userId, UUID projectId) {
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        if (!project.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return project;
    }

    private String filename(String title, ExportFormat format) {
        return filenameBase(title) + "." + format.extension();
    }

    private String filenameBase(String title) {
        String base = title == null ? "proyecto" : title;
        String sanitized = base.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (sanitized.isBlank() || sanitized.equals(".") || sanitized.equals("..")) {
            sanitized = "proyecto";
        }
        if (sanitized.length() > 60) {
            sanitized = sanitized.substring(0, 60);
        }
        return "KIN_" + sanitized;
    }
}
