package com.kinplatform.billing.authorization;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MipresControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MipresService mipresService;

    @Mock
    private org.springframework.security.core.Authentication authentication;

    private UUID orgId;
    private UUID contractId;
    private UUID patientId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        contractId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        TenantContext.set(orgId);

        var controller = new MipresController(mipresService, mock(UserRepository.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createPrescription_returns201() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        String authNumber = "AUTH-2024-001";

        var prescription = com.kinplatform.billing.authorization.MipresPrescription.builder()
                .id(prescriptionId)
                .organizationId(TenantContext.get())
                .contractId(contractId)
                .patientId(patientId)
                .prescriptionNumber(authNumber)
                .prescriptionDate(LocalDate.now())
                .status(com.kinplatform.billing.authorization.MipresPrescription.PrescriptionStatus.AUTHORIZED)
                .cupsCode("890201")
                .build();

        when(mipresService.createPrescription(any(), any()))
                .thenReturn(prescription);

        mockMvc.perform(post("/billing/mipres/prescriptions")
                        .contentType("application/json")
                        .content("""
                            {"authorizationNumber":"AUTH-2024-001","contractId":"%s","patientId":"%s"}
                            """.formatted(contractId, patientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptionNumber").value(authNumber))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void getPrescription_returns200() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        var prescription = com.kinplatform.billing.authorization.MipresPrescription.builder()
                .id(prescriptionId)
                .prescriptionNumber("AUTH-2024-001")
                .status(com.kinplatform.billing.authorization.MipresPrescription.PrescriptionStatus.AUTHORIZED)
                .build();

        when(mipresService.getPrescription(prescriptionId))
                .thenReturn(Optional.of(prescription));

        mockMvc.perform(get("/billing/mipres/prescriptions/{id}", prescriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prescriptionId.toString()));
    }

    @Test
    void getPrescription_notFound_returns404() throws Exception {
        when(mipresService.getPrescription(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/billing/mipres/prescriptions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPrescriptions_returns200() throws Exception {
        var prescription = com.kinplatform.billing.authorization.MipresPrescription.builder()
                .id(UUID.randomUUID())
                .prescriptionNumber("AUTH-2024-001")
                .status(com.kinplatform.billing.authorization.MipresPrescription.PrescriptionStatus.AUTHORIZED)
                .build();

        when(mipresService.getPrescriptions(any(), any(), any()))
                .thenReturn(List.of(prescription));

        mockMvc.perform(get("/billing/mipres/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prescriptionNumber").value("AUTH-2024-001"));
    }

    @Test
    void reportSupply_returns201() throws Exception {
        UUID supplyId = UUID.randomUUID();
        var supply = com.kinplatform.billing.authorization.MipresSupply.builder()
                .id(supplyId)
                .prescriptionNumber("AUTH-2024-001")
                .supplyDate(java.time.LocalDate.now())
                .status(com.kinplatform.billing.authorization.MipresSupply.SupplyStatus.REPORTED)
                .build();

        when(mipresService.reportSupply(any(), any()))
                .thenReturn(supply);

        mockMvc.perform(post("/billing/mipres/supplies")
                        .contentType("application/json")
                        .content("""
                            {"authorizationNumber":"AUTH-2024-001","cupsCode":"890201","quantity":5,"value":50000,"unitValueCop":10000,"batchNumber":"LOTE-123","expirationDate":"%s"}
                            """.formatted(LocalDate.now().plusMonths(12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplyId").exists());
    }

    @Test
    void anularSupply_returns200() throws Exception {
        UUID supplyId = UUID.randomUUID();
        var supply = com.kinplatform.billing.authorization.MipresSupply.builder()
                .id(supplyId)
                .status(com.kinplatform.billing.authorization.MipresSupply.SupplyStatus.ANULLED)
                .build();

        when(mipresService.anularSupply(any(), eq(supplyId), anyString()))
                .thenReturn(supply);

        mockMvc.perform(put("/billing/mipres/supplies/{id}/anular", supplyId)
                        .param("motivo", "Error en cantidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULLED"));
    }

    @Test
    void listSupplies_returns200() throws Exception {
        var supply = com.kinplatform.billing.authorization.MipresSupply.builder()
                .id(UUID.randomUUID())
                .prescriptionNumber("AUTH-2024-001")
                .status(com.kinplatform.billing.authorization.MipresSupply.SupplyStatus.REPORTED)
                .build();

        when(mipresService.getSupplies(any(), any(), any(), any()))
                .thenReturn(List.of(supply));

        mockMvc.perform(get("/billing/mipres/supplies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prescriptionNumber").value("AUTH-2024-001"));
    }

    @Test
    void getConsolidatedReport_returns200() throws Exception {
        mockMvc.perform(get("/billing/mipres/reports/consolidated")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSupplies").exists())
                .andExpect(jsonPath("$.byStatus").exists());
    }
}