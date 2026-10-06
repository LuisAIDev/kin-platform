package com.kinplatform.kin.export.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.export.model.ExportFormat;
import com.kinplatform.kin.export.model.ExportMode;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.projectdoc.ProjectDocument;
import com.kinplatform.projectdoc.ProjectDocumentService;
import com.kinplatform.projectdoc.ProjectDocumentStatus;
import com.kinplatform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.projectinfo.ProjectStructuredInfoRepository;
import com.kinplatform.projectinfo.StructuredInfoSourceType;
import com.kinplatform.user.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * La capa de aplicación verifica ownership y exporta usando datos reales del
 * proyecto (no el historial). Proyecto ajeno o inexistente → 404. Con plantilla,
 * el documento de referencia debe pertenecer al proyecto y estar procesado.
 */
class ProjectExportServiceImplTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ProjectStructuredInfoRepository infoRepository = mock(ProjectStructuredInfoRepository.class);
    private final ProjectDocumentService documentService = mock(ProjectDocumentService.class);

    private ProjectExportService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProjectExportServiceImpl(projectRepository, reportRepository, infoRepository, documentService);
    }

    private Project project() {
        User user = User.builder().id(userId).email("owner@kin.com").build();
        return Project.builder()
                .id(projectId)
                .user(user)
                .title("CAFÉ MARTE 777")
                .description("cafetería espacial orientada a astronautas")
                .build();
    }

    @Test
    void exportaProyectoPropioEnDocx() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        when(reportRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(infoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of());

        ExportResult result = service.export(userId, projectId, ExportFormat.DOCX, ExportMode.COMPLETE, null);

        assertNotNull(result.bytes());
        assertTrue(result.bytes().length > 0);
        assertTrue(result.filename().endsWith(".docx"));
        assertTrue(result.bytes()[0] == 'P' && result.bytes()[1] == 'K');
    }

    @Test
    void exportaProyectoPropioEnMarkdownConDatosReales() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        when(reportRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(infoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of(ProjectStructuredInfo.builder()
                        .projectId(projectId)
                        .section("MERCADO")
                        .key("mercado_objetivo")
                        .value("astronautas")
                        .sourceType(StructuredInfoSourceType.USER_INPUT)
                        .build()));

        ExportResult result = service.export(userId, projectId, ExportFormat.MARKDOWN, ExportMode.COMPLETE, null);

        String md = new String(result.bytes(), StandardCharsets.UTF_8);
        assertTrue(md.contains("CAFÉ MARTE 777"));
        assertTrue(md.contains("mercado_objetivo"));
        assertTrue(md.contains("astronautas"));
        assertFalse(md.contains("Historial de Conversación"));
    }

    @Test
    void proyectoSinReporteNiInfoSeExportaSinInventar() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        when(reportRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(infoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of());

        ExportResult result = service.export(userId, projectId, ExportFormat.MARKDOWN, ExportMode.COMPLETE, null);

        String md = new String(result.bytes(), StandardCharsets.UTF_8);
        assertTrue(md.contains("CAFÉ MARTE 777"));
        assertTrue(md.contains("cafetería espacial"));
    }

    @Test
    void proyectoAjenoDevuelve404() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(UUID.randomUUID(), projectId, ExportFormat.PDF, ExportMode.COMPLETE, null));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void proyectoInexistenteDevuelve404() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(userId, projectId, ExportFormat.PDF, ExportMode.COMPLETE, null));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void optionsReflejaAusenciaDeReporte() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        when(reportRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(infoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of());

        ExportOptions options = service.options(userId, projectId);

        assertFalse(options.hasReport());
        assertTrue(options.formats().contains("DOCX"));
        assertTrue(options.formats().contains("PDF"));
        assertTrue(options.formats().contains("MARKDOWN"));
        assertEquals("KIN_caf_marte_777", options.filenameBase());
    }

    @Test
    void plantillaProcesadaGeneraMarkdownConEstructuraDelDocumento() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        when(reportRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(infoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of());
        ProjectDocument doc = ProjectDocument.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .filename("Formato SENA.docx")
                .mimeType("application/vnd...")
                .status(ProjectDocumentStatus.PROCESADO)
                .extractedText("PORTADA\n\n1. INTRODUCCIÓN\n\n2. PRESUPUESTO\n\n3. CONCLUSIONES")
                .build();
        when(documentService.findOwned(userId, projectId, doc.getId())).thenReturn(Optional.of(doc));

        ExportResult result =
                service.export(userId, projectId, ExportFormat.MARKDOWN, ExportMode.COMPLETE, doc.getId());

        String md = new String(result.bytes(), StandardCharsets.UTF_8);
        assertTrue(md.contains("## 1. INTRODUCCIÓN"));
        assertTrue(md.contains("## 2. PRESUPUESTO"));
        assertTrue(md.contains("## 3. CONCLUSIONES"));
        assertTrue(md.contains("cafetería espacial")); // datos del proyecto
        assertFalse(md.contains("PORTADA\n\n1. INTRODUCCIÓN"));
    }

    @Test
    void plantillaConDatosDelProyectoNoCopiaContenidoDelDocumento() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        when(reportRepository.findLatest(projectId)).thenReturn(Optional.empty());
        when(infoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of());
        ProjectDocument doc = ProjectDocument.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .filename("Plantilla.docx")
                .mimeType("text/plain")
                .status(ProjectDocumentStatus.PROCESADO)
                .extractedText("1. INTRODUCCIÓN\n\nTexto literal del documento de referencia que NO debe copiarse.")
                .build();
        when(documentService.findOwned(userId, projectId, doc.getId())).thenReturn(Optional.of(doc));

        ExportResult result =
                service.export(userId, projectId, ExportFormat.MARKDOWN, ExportMode.COMPLETE, doc.getId());

        String md = new String(result.bytes(), StandardCharsets.UTF_8);
        assertFalse(md.contains("Texto literal del documento de referencia"));
    }

    @Test
    void documentoErrorComoPlantillaDevuelve400() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        ProjectDocument doc = ProjectDocument.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .filename("roto.pdf")
                .mimeType("application/pdf")
                .status(ProjectDocumentStatus.ERROR)
                .extractedText("")
                .build();
        when(documentService.findOwned(userId, projectId, doc.getId())).thenReturn(Optional.of(doc));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(userId, projectId, ExportFormat.PDF, ExportMode.COMPLETE, doc.getId()));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void documentoSinTextoExtraidoComoPlantillaDevuelve400() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        ProjectDocument doc = ProjectDocument.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .filename("vacio.docx")
                .mimeType("application/vnd...")
                .status(ProjectDocumentStatus.PROCESADO)
                .extractedText(null)
                .build();
        when(documentService.findOwned(userId, projectId, doc.getId())).thenReturn(Optional.of(doc));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(userId, projectId, ExportFormat.PDF, ExportMode.COMPLETE, doc.getId()));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void documentoInexistenteComoPlantillaDevuelve404() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        UUID documentId = UUID.randomUUID();
        when(documentService.findOwned(userId, projectId, documentId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(userId, projectId, ExportFormat.PDF, ExportMode.COMPLETE, documentId));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void documentoDeOtroProyectoComoPlantillaDevuelve404() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        UUID documentId = UUID.randomUUID();
        // findOwned filtra por projectId: el documento de otro proyecto no se expone
        when(documentService.findOwned(userId, projectId, documentId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(userId, projectId, ExportFormat.PDF, ExportMode.COMPLETE, documentId));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void proyectoAjenoConPlantillaDevuelve404() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project()));
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.export(
                        UUID.randomUUID(), projectId, ExportFormat.PDF, ExportMode.COMPLETE, UUID.randomUUID()));
        assertEquals(404, ex.getStatusCode().value());
    }
}

