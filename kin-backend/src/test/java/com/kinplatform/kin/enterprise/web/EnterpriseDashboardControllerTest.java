package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.application.InMemoryEnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnterpriseDashboardControllerTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private InMemoryEnterpriseProjectRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnterpriseProjectRepository();
        var controller = new EnterpriseDashboardController(repository, new EnterpriseWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new EnterpriseApiExceptionHandler())
            .build();
    }

    @Test
    void getDashboard_deberiaDevolverLaVistaConsolidada() throws Exception {
        repository.save(com.kinplatform.kin.enterprise.web.WebTestFixtures.completed(
            PROJECT_ID, 1, DocumentType.LEAN_CANVAS, DocumentType.KPI));
        repository.save(com.kinplatform.kin.enterprise.web.WebTestFixtures.completed(
            PROJECT_ID, 2, DocumentType.ROADMAP));

        mockMvc.perform(get("/enterprise/{projectId}/2/dashboard", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.progress").value(100))
            .andExpect(jsonPath("$.documentCount").value(1))
            .andExpect(jsonPath("$.versionsCount").value(2))
            .andExpect(jsonPath("$.documents[0].type").value("ROADMAP"))
            .andExpect(jsonPath("$.versions[0].version").value(1))
            .andExpect(jsonPath("$.versions[1].version").value(2))
            .andExpect(jsonPath("$.statistics.documentCount").value(1))
            .andExpect(jsonPath("$.statistics.versionsCount").value(2))
            .andExpect(jsonPath("$.statistics.totalBytes").isNumber());
    }

    @Test
    void getDashboard_conVersionRunning_deberiaReflejarProgresoParcial() throws Exception {
        var now = java.time.OffsetDateTime.now();
        repository.save(com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.start(
            PROJECT_ID, 1, now, now, java.util.List.of()));

        mockMvc.perform(get("/enterprise/{projectId}/1/dashboard", PROJECT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.progress").value(40));
    }

    @Test
    void getDashboard_versionInexistente_deberiaDevolver404() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/1/dashboard", PROJECT_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void getDashboard_versionInvalida_deberiaDevolver400() throws Exception {
        mockMvc.perform(get("/enterprise/{projectId}/0/dashboard", PROJECT_ID))
            .andExpect(status().isBadRequest());
    }

    @Test
    void constructor_conDependenciaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseDashboardController(null, new EnterpriseWebMapper()));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseDashboardController(repository, null));
    }
}
