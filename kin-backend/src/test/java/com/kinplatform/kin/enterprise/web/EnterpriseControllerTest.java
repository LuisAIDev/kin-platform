package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.application.EnterpriseExportOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationOrchestrator;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationRequest;
import com.kinplatform.kin.enterprise.application.EnterpriseGenerationService;
import com.kinplatform.kin.enterprise.application.InMemoryEnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnterpriseControllerTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private InMemoryEnterpriseProjectRepository repository;
    private ContextRepository contextRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnterpriseProjectRepository();
        contextRepository = mock(ContextRepository.class);
        when(contextRepository.find(PROJECT_ID))
            .thenReturn(Optional.of(EngineTestFixtures.contextWithAll()));
        var generation = new EnterpriseGenerationService(
            new EnterpriseDocumentAssembler(), repository, new InMemoryDomainEventBus());
        var controller = new EnterpriseController(repository, contextRepository,
            new EnterpriseExportOrchestrator(repository),
            new EnterpriseGenerationOrchestrator(generation),
            new EnterpriseWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new EnterpriseApiExceptionHandler())
            .build();
    }

    private void seedCompleted(int version, DocumentType... types) {
        repository.save(WebTestFixtures.completed(PROJECT_ID, version, types));
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Test
    void getSummary_deberiaDevolverElResumenDeLaUltimaVersion() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.documentCount").value(1));
    }

    @Test
    void getSummary_sinVersiones_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}", PROJECT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getLatest_deberiaDevolverLaVersionCompleta() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS, DocumentType.KPI);

        mockMvc.perform(get("/enterprise/{projectId}/latest", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.documents", hasSize(2)))
            .andExpect(jsonPath("$.documents[0].type").value("LEAN_CANVAS"));
    }

    @Test
    void getLatest_sinVersiones_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/latest", PROJECT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void getVersions_deberiaDevolverLasVersionesOrdenadas() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);
        seedCompleted(2, DocumentType.KPI);

        mockMvc.perform(get("/enterprise/{projectId}/versions", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].version").value(1))
            .andExpect(jsonPath("$[1].version").value(2));
    }

    @Test
    void getVersions_sinVersiones_deberiaDevolverListaVacia() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/versions", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getByVersion_deberiaDevolverLaVersionSolicitada() throws Exception {
        seedCompleted(3, DocumentType.ROADMAP);

        mockMvc.perform(get("/enterprise/{projectId}/{version}", PROJECT_ID, 3))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(3))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getByVersion_inexistente_deberiaDevolver404() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}", PROJECT_ID, 99))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getByVersion_invalida_deberiaDevolver400() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/{version}", PROJECT_ID, 0))
            .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Generación
    // ------------------------------------------------------------------

    @Test
    void generate_bloqueante_deberiaGenerarYDevolver201() throws Exception {
        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":false}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.documents", hasSize(7)));
    }

    @Test
    void generate_asincrono_deberiaDevolver202() throws Exception {
        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":true}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void generate_conGeneracionEnCurso_deberiaDevolver409() throws Exception {
        repository.save(EnterpriseProject.request(PROJECT_ID, 1));

        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":false}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void generate_sinContexto_deberiaDevolver422() throws Exception {
        when(contextRepository.find(PROJECT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":false}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void generate_conSolicitudInvalida_deberiaDevolver400() throws Exception {
        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.async").exists());
    }

    @Test
    void generate_conVersionInvalida_deberiaDevolver400() throws Exception {
        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":false,\"requestedVersion\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void generate_conVersionSolicitada_deberiaRegenerarEsaVersion() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":false,\"requestedVersion\":2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void generate_generacionFallida_deberiaDevolver422() throws Exception {
        var failed = EnterpriseProject.fail(PROJECT_ID, 1,
            java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(),
            "fallo del motor", java.util.List.of());
        var mockGeneration = mock(EnterpriseGenerationOrchestrator.class);
        when(mockGeneration.generate(
            org.mockito.ArgumentMatchers.any(EnterpriseGenerationRequest.class))).thenReturn(failed);
        var controller = new EnterpriseController(repository, contextRepository,
            new EnterpriseExportOrchestrator(repository), mockGeneration,
            new EnterpriseWebMapper());
        var mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new EnterpriseApiExceptionHandler())
            .build();

        mvc.perform(post("/enterprise/{projectId}/generate", PROJECT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"async\":false}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("FAILED"));
    }

    // ------------------------------------------------------------------
    // Estado y documentos
    // ------------------------------------------------------------------

    @Test
    void getStatus_deberiaDevolverElEstado() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/status", PROJECT_ID, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()));
    }

    @Test
    void getStatus_inexistente_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/{version}/status", PROJECT_ID, 1))
            .andExpect(status().isNotFound());
    }

    @Test
    void getDocuments_deberiaDevolverLosDocumentos() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS, DocumentType.KPI);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/documents", PROJECT_ID, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].type").value("LEAN_CANVAS"))
            .andExpect(jsonPath("$[0].size").isNumber());
    }

    @Test
    void getDocuments_inexistente_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/{version}/documents", PROJECT_ID, 1))
            .andExpect(status().isNotFound());
    }

    @Test
    void getDocument_deberiaDevolverLosMetadatos() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/documents/{type}",
                PROJECT_ID, 1, "LEAN_CANVAS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("LEAN_CANVAS"))
            .andExpect(jsonPath("$.generatedBy").value("BusinessModelEngine"));
    }

    @Test
    void getDocument_tipoAusente_deberiaDevolver404() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/documents/{type}",
                PROJECT_ID, 1, "KPI"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getDocument_tipoInvalido_deberiaDevolver400() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/documents/{type}",
                PROJECT_ID, 1, "NO_EXISTE"))
            .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Exportación
    // ------------------------------------------------------------------

    @Test
    void getExport_deberiaDevolverLosTamanoPorFormato() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export", PROJECT_ID, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.documents.LEAN_CANVAS.PDF").isNumber())
            .andExpect(jsonPath("$.documents.LEAN_CANVAS.DOCX").isNumber())
            .andExpect(jsonPath("$.documents.LEAN_CANVAS.PPTX").isNumber());
    }

    @Test
    void getExport_inexistente_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/{version}/export", PROJECT_ID, 1))
            .andExpect(status().isNotFound());
    }

    @Test
    void exportFormat_deberiaDevolverUnZip() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS, DocumentType.KPI);

        var response = mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{format}",
                PROJECT_ID, 1, "PDF"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"))
            .andExpect(header().string("Content-Disposition", startsWith("attachment")))
            .andReturn().getResponse();

        byte[] bytes = response.getContentAsByteArray();
        assertTrue(bytes.length > 0);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    @Test
    void exportFormat_formatoInvalido_deberiaDevolver400() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{format}",
                PROJECT_ID, 1, "HTML"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void exportFormat_versionInexistente_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{format}",
                PROJECT_ID, 1, "PDF"))
            .andExpect(status().isNotFound());
    }

    @Test
    void exportFormat_deberiaOmitirLosDocumentosSinElFormato() throws Exception {
        var bundle = com.kinplatform.kin.enterprise.application.EnterpriseDocumentBundle.of(
            PROJECT_ID, 1, java.util.Map.of(
                com.kinplatform.kin.enterprise.valueobjects.DocumentType.LEAN_CANVAS,
                    java.util.Map.of(RenderFormat.DOCX, new byte[]{1}),
                com.kinplatform.kin.enterprise.valueobjects.DocumentType.KPI,
                    java.util.Map.of(RenderFormat.PDF, new byte[]{2})));
        var mockExport = mock(EnterpriseExportOrchestrator.class);
        when(mockExport.export(PROJECT_ID, 1)).thenReturn(bundle);
        var controller = new EnterpriseController(repository, contextRepository,
            mockExport,
            new EnterpriseGenerationOrchestrator(new EnterpriseGenerationService(
                new EnterpriseDocumentAssembler(), repository, new InMemoryDomainEventBus())),
            new EnterpriseWebMapper());
        var mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new EnterpriseApiExceptionHandler())
            .build();

        mvc.perform(get("/enterprise/{projectId}/{version}/export/{format}",
                PROJECT_ID, 1, "PDF"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"));
    }

    @Test
    void exportDocument_deberiaDevolverLosBytesPdf() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{type}/{format}",
                PROJECT_ID, 1, "LEAN_CANVAS", "PDF"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition",
                "attachment; filename=\"lean_canvas.pdf\""));
    }

    @Test
    void exportDocument_deberiaDevolverLosBytesDocx() throws Exception {
        seedCompleted(1, DocumentType.FINANCIAL_PLAN);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{type}/{format}",
                PROJECT_ID, 1, "FINANCIAL_PLAN", "DOCX"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void exportDocument_deberiaDevolverLosBytesPptx() throws Exception {
        seedCompleted(1, DocumentType.ROADMAP);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{type}/{format}",
                PROJECT_ID, 1, "ROADMAP", "PPTX"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    }

    @Test
    void exportDocument_tipoAusente_deberiaDevolver404() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{type}/{format}",
                PROJECT_ID, 1, "KPI", "PDF"))
            .andExpect(status().isNotFound());
    }

    @Test
    void exportDocument_tipoInvalido_deberiaDevolver400() throws Exception {
        seedCompleted(1, DocumentType.LEAN_CANVAS);

        mockMvc.perform(get("/enterprise/{projectId}/{version}/export/{type}/{format}",
                PROJECT_ID, 1, "NO_EXISTE", "PDF"))
            .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    @Test
    void constructor_conDependenciaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseController(null, contextRepository,
                new EnterpriseExportOrchestrator(repository),
                new EnterpriseGenerationOrchestrator(
                    new EnterpriseGenerationService(new EnterpriseDocumentAssembler(),
                        repository, new InMemoryDomainEventBus())),
                new EnterpriseWebMapper()));
    }
}
