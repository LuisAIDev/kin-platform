package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.dto.DiagnosesResponse;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.service.DiagnosesService;
import com.kinplatform.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DiagnosesController.class)
@ContextConfiguration(classes = {DiagnosesController.class, DiagnosesControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class DiagnosesControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiagnosesService diagnosesService;

    private UUID encounterId;
    private UUID diagnosisId;
    private UUID patientId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        diagnosisId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CreateDiagnosisRequest validRequest() {
        return CreateDiagnosisRequest.builder()
                .encounterId(encounterId)
                .cie10Code("K59.0")
                .cie10Description("Constipación")
                .diagnosisType(Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(Diagnoses.Certainty.CONFIRMED)
                .classification(Diagnoses.Classification.CONSULTA)
                .supportedBy("Examen físico y laboratorio")
                .onsetDate(LocalDate.now())
                .status(Diagnoses.Status.ACTIVE)
                .notes("Paciente con estreñimiento crónico")
                .build();
    }

    private DiagnosesResponse sampleResponse() {
        return DiagnosesResponse.builder()
                .id(diagnosisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.0")
                .cie10Description("Constipación")
                .diagnosisType(Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(Diagnoses.Certainty.CONFIRMED)
                .classification(Diagnoses.Classification.CONSULTA)
                .supportedBy("Examen físico y laboratorio")
                .onsetDate(LocalDate.now())
                .status(Diagnoses.Status.ACTIVE)
                .notes("Paciente con estreñimiento crónico")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDiagnosis_happyPath_returns201() throws Exception {
        when(diagnosesService.addDiagnosis(any(CreateDiagnosisRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.cie10Code").value("K59.0"))
                .andExpect(jsonPath("$.diagnosisType").value("PRINCIPAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDiagnosis_validationError_returns400() throws Exception {
        CreateDiagnosisRequest invalid = CreateDiagnosisRequest.builder().build();

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDiagnosis_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createDiagnosis_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void createDiagnosis_ipsAdminRole_allowed() throws Exception {
        when(diagnosesService.addDiagnosis(any(CreateDiagnosisRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void listDiagnoses_happyPath_returns200() throws Exception {
        DiagnosesResponse comorbilidad = DiagnosesResponse.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("E11.9")
                .diagnosisType(Diagnoses.DiagnosisType.COMORBILIDAD)
                .certainty(Diagnoses.Certainty.CONFIRMED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(diagnosesService.getAllByEncounter(encounterId))
                .thenReturn(List.of(sampleResponse(), comorbilidad));

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/diagnoses", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisType").value("PRINCIPAL"))
                .andExpect(jsonPath("$[1].diagnosisType").value("COMORBILIDAD"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void setPrincipal_happyPath_returns200() throws Exception {
        when(diagnosesService.setPrincipal(diagnosisId)).thenReturn(sampleResponse());

        mockMvc.perform(put("/health/hce/diagnoses/{diagnosisId}/principal", diagnosisId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$.diagnosisType").value("PRINCIPAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getPrincipal_happyPath_returns200() throws Exception {
        when(diagnosesService.getPrincipal(encounterId)).thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/diagnoses/principal", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$.diagnosisType").value("PRINCIPAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getPrincipal_notFound_returns404() throws Exception {
        when(diagnosesService.getPrincipal(encounterId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/diagnoses/principal", encounterId))
                .andExpect(status().isNotFound());
    }
}
