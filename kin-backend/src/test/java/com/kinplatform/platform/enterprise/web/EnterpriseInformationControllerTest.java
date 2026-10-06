package com.kinplatform.platform.enterprise.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.platform.enterprise.integration.DocumentDescriptor;
import com.kinplatform.platform.enterprise.integration.EnterpriseDataProvider;
import com.kinplatform.platform.enterprise.integration.EnterpriseIntegrationData;
import com.kinplatform.platform.enterprise.integration.ResolvedContext;
import com.kinplatform.platform.enterprise.integration.StructuredDatum;
import com.kinplatform.platform.enterprise.integration.SupplementalData;
import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EnterpriseInformationControllerTest {

    private final EnterpriseDataProvider dataProvider = mock(EnterpriseDataProvider.class);
    private final ContextRepository contextRepository = mock(ContextRepository.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new EnterpriseInformationController(dataProvider, contextRepository))
            .build();

    private final UUID projectId = UUID.randomUUID();

    @Test
    void informationDevuelveDimensionesSuplementariosYDocumentos() throws Exception {
        when(contextRepository.find(projectId)).thenReturn(Optional.empty());
        List<StructuredDatum> data = List.of(
                new StructuredDatum("FINANZAS", "inversion_inicial", "80000000", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum(
                        "MERCADO", "competidores", "Competidor A", StructuredInfoSourceType.IMPORTED_DOCUMENT));
        when(dataProvider.load(projectId, null))
                .thenReturn(new EnterpriseIntegrationData(
                        ResolvedContext.resolve(null, data),
                        SupplementalData.from(data),
                        List.of(new DocumentDescriptor(
                                "d1",
                                "plan.pdf",
                                "application/pdf",
                                100,
                                "PROCESADO",
                                "2026-08-10",
                                "resumen",
                                List.of("inversión")))));

        mockMvc.perform(get("/enterprise/{id}/information", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedDimensions").isArray())
                .andExpect(jsonPath("$.supplemental.financial.inversion_inicial.value")
                        .value("80000000"))
                .andExpect(jsonPath("$.documents[0].filename").value("plan.pdf"));
    }

    @Test
    void dimensionAusenteSeReportaPendienteNuncaCero() throws Exception {
        when(contextRepository.find(projectId)).thenReturn(Optional.empty());
        when(dataProvider.load(projectId, null))
                .thenReturn(new EnterpriseIntegrationData(
                        ResolvedContext.resolve(null, List.of()), SupplementalData.from(List.of()), List.of()));

        mockMvc.perform(get("/enterprise/{id}/information", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedDimensions[?(@.dimension=='MVP')].state")
                        .value("PENDING"))
                .andExpect(jsonPath("$.resolvedDimensions[?(@.dimension=='MVP')].value")
                        .value((String) null));
    }
}




