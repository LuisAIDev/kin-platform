package com.kinplatform.platform.export.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.platform.export.application.ExportOptions;
import com.kinplatform.platform.export.application.ExportResult;
import com.kinplatform.platform.export.application.ProjectExportService;
import com.kinplatform.platform.export.model.ExportFormat;
import com.kinplatform.platform.export.model.ExportMode;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Los endpoints de exportación exigen autenticación, verifican ownership y
 * sirven los documentos en el formato solicitado.
 */
class ProjectExportControllerTest {

    private final ProjectExportService exportService = mock(ProjectExportService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Authentication auth = mock(Authentication.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new ProjectExportController(exportService, userRepository))
            .build();

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        User user = User.builder().id(userId).email("owner@kin.com").build();
        when(auth.getName()).thenReturn("owner@kin.com");
        when(userRepository.findByEmail("owner@kin.com")).thenReturn(Optional.of(user));
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken("owner@kin.com", null, List.of());
    }

    @Test
    void optionsDevuelveFormatosDisponibles() throws Exception {
        when(exportService.options(userId, projectId))
                .thenReturn(new ExportOptions(
                        List.of("DOCX", "PDF", "MARKDOWN"),
                        true,
                        List.of("Resumen Ejecutivo"),
                        List.of("MERCADO"),
                        "KIN_caf_marte_777"));

        mockMvc.perform(get("/projects/{id}/export", projectId).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formats[0]").value("DOCX"))
                .andExpect(jsonPath("$.hasReport").value(true))
                .andExpect(jsonPath("$.filenameBase").value("KIN_caf_marte_777"));
    }

    @Test
    void exportDocxDevuelveArchivoWord() throws Exception {
        when(exportService.export(eq(userId), eq(projectId), eq(ExportFormat.DOCX), eq(ExportMode.COMPLETE), isNull()))
                .thenReturn(new ExportResult(ExportFormat.DOCX, new byte[] {'P', 'K', 0, 0}, "KIN_test.docx"));

        mockMvc.perform(get("/projects/{id}/export/docx", projectId).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(new byte[] {'P', 'K', 0, 0}));
    }

    @Test
    void exportPdfDevuelveDocumentoPdf() throws Exception {
        when(exportService.export(eq(userId), eq(projectId), eq(ExportFormat.PDF), eq(ExportMode.COMPLETE), isNull()))
                .thenReturn(new ExportResult(
                        ExportFormat.PDF, "%PDF-1.4".getBytes(StandardCharsets.US_ASCII), "KIN_test.pdf"));

        mockMvc.perform(get("/projects/{id}/export/pdf", projectId).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void exportMarkdownDevuelveTextoMarkdown() throws Exception {
        when(exportService.export(
                        eq(userId), eq(projectId), eq(ExportFormat.MARKDOWN), eq(ExportMode.COMPLETE), isNull()))
                .thenReturn(new ExportResult(
                        ExportFormat.MARKDOWN, "# Test".getBytes(StandardCharsets.UTF_8), "KIN_test.md"));

        mockMvc.perform(get("/projects/{id}/export/markdown", projectId).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8")))
                .andExpect(content().string("# Test"));
    }

    @Test
    void exportResumenUsaModoSummary() throws Exception {
        when(exportService.export(
                        eq(userId), eq(projectId), eq(ExportFormat.MARKDOWN), eq(ExportMode.SUMMARY), isNull()))
                .thenReturn(new ExportResult(
                        ExportFormat.MARKDOWN, "# Resumen".getBytes(StandardCharsets.UTF_8), "KIN_test.md"));

        mockMvc.perform(get("/projects/{id}/export/markdown?mode=summary", projectId)
                        .principal(principal()))
                .andExpect(status().isOk());
    }

    @Test
    void proyectoAjenoDevuelve404() throws Exception {
        when(exportService.options(any(UUID.class), eq(projectId)))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Proyecto no encontrado"));

        mockMvc.perform(get("/projects/{id}/export", projectId).principal(principal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void formatoNoSoportadoDevuelve400() throws Exception {
        mockMvc.perform(get("/projects/{id}/export/xlsx", projectId).principal(principal()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modoNoSoportadoDevuelve400() throws Exception {
        mockMvc.perform(get("/projects/{id}/export/markdown?mode=completo", projectId)
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportConPlantillaPasaElDocumentoAlServicio() throws Exception {
        UUID docId = UUID.randomUUID();
        when(exportService.export(
                        eq(userId), eq(projectId), eq(ExportFormat.MARKDOWN), eq(ExportMode.COMPLETE), eq(docId)))
                .thenReturn(new ExportResult(
                        ExportFormat.MARKDOWN, "# Estructura".getBytes(StandardCharsets.UTF_8), "KIN_test.md"));

        mockMvc.perform(get("/projects/{id}/export/markdown", projectId)
                        .param("templateDocumentId", docId.toString())
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(content().string("# Estructura"));
    }

    @Test
    void plantillaRechazadaPropaga400() throws Exception {
        UUID docId = UUID.randomUUID();
        when(exportService.export(eq(userId), eq(projectId), eq(ExportFormat.DOCX), eq(ExportMode.COMPLETE), eq(docId)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El documento seleccionado no puede utilizarse como plantilla."));

        mockMvc.perform(get("/projects/{id}/export/docx", projectId)
                        .param("templateDocumentId", docId.toString())
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }
}



