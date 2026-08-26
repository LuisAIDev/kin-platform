package com.kinplatform.kin.health.triage.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.kin.health.triage.adapter.HealthDataImporter;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.domain.ValidationStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de los endpoints administrativos del catálogo (ADR-028, fase de
 * consolidación) con MockMvc standalone.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TriageCatalogAdminControllerTest {

    @Mock
    private TriageCatalogUpdateService catalogUpdateService;

    @Mock
    private HealthDataImporter healthDataImporter;

    @Mock
    private CoverageReportService coverageReportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TriageCatalogAdminController(
                        catalogUpdateService, healthDataImporter, coverageReportService))
                .build();
    }

    @Test
    void updateCatalog_deberiaDevolverConteos() throws Exception {
        when(catalogUpdateService.updateCatalog()).thenReturn(CatalogUpdateResult.of(3, 5, 12, "health-catalog"));

        mockMvc.perform(post("/admin/health/triage/catalog/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptomsAdded").value(3))
                .andExpect(jsonPath("$.conditionsAdded").value(5))
                .andExpect(jsonPath("$.relationsAdded").value(12))
                .andExpect(jsonPath("$.source").value("health-catalog"))
                .andExpect(jsonPath("$.changed").value(true));
    }

    @Test
    void updateCatalog_sinCambios_deberiaDevolverChangedFalse() throws Exception {
        when(catalogUpdateService.updateCatalog()).thenReturn(CatalogUpdateResult.empty("Sin datos de fuente externa"));

        mockMvc.perform(post("/admin/health/triage/catalog/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(false));
    }

    @Test
    void importFromExternal_deberiaDevolverConteos() throws Exception {
        when(healthDataImporter.importFromExternal()).thenReturn(CatalogUpdateResult.of(2, 4, 8, "health-importer"));

        mockMvc.perform(post("/admin/health/catalog/import-from-external"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionsAdded").value(4))
                .andExpect(jsonPath("$.source").value("health-importer"));
    }

    @Test
    void coverage_deberiaDevolverInforme() throws Exception {
        var report = new CoverageReportService.CoverageReport(
                100,
                80,
                20,
                List.of(new CoverageReportService.CoverageItem(
                        UUID.randomUUID(), "Gripe", "J11", 6, ValidationStatus.APPROVED.name(), false)));
        when(coverageReportService.generate()).thenReturn(report);

        mockMvc.perform(get("/admin/health/catalog/coverage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConditions").value(100))
                .andExpect(jsonPath("$.insufficientConditions").value(20))
                .andExpect(jsonPath("$.items[0].name").value("Gripe"))
                .andExpect(jsonPath("$.items[0].validationStatus").value("APPROVED"));
    }
}
