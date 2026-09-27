package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.DiagnosisResponse;
import com.kinplatform.kin.health.hce.dto.request.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.entity.Diagnosis;
import com.kinplatform.kin.health.hce.service.DiagnosisService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private DiagnosisService diagnosisService;

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

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDiagnosis_happyPath_returns201() throws Exception {
        CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                .encounterId(encounterId)
                .cie10Code("K59.0")
                .cie10Description("Constipación")
                .diagnosisType("PRINCIPAL")
                .certainty("CONFIRMED")
                .classification("CONSULTA")
                .supportedBy("Examen físico y laboratorio")
                .onsetDate(LocalDate.now())
                .status("ACTIVE")
                .notes("Paciente con estreñimiento crónico")
                .build();

        DiagnosisResponse response = DiagnosisResponse.builder()
                .id(diagnosisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.0")
                .cie10Description("Constipación")
                .diagnosisType(Diagnosis.DiagnosisType.PRINCIPAL)
                .certainty(Diagnosis.Certainty.CONFIRMED)
                .classification(Diagnosis.Classification.CONSULTA)
                .supportedBy("Examen físico y laboratorio")
                .onsetDate(LocalDate.now())
                .status(Diagnosis.Status.ACTIVE)
                .notes("Paciente con estreñimiento crónico")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(diagnosisService.createDiagnosis(any(CreateDiagnosisRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.cie10Code").value("K59.0"))
                .andExpect(jsonPath("$.diagnosisType").value("PRINCIPAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDiagnosis_validationError_returns400() throws Exception {
        CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                .encounterId(null)
                .build();

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDiagnosis_unauthenticated_returns401() throws Exception {
        CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                .encounterId(encounterId)
                .cie10Code("K59.0")
                .diagnosisType("PRINCIPAL")
                .certainty("CONFIRMED")
                .build();

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createDiagnosis_wrongRole_returns403() throws Exception {
        CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                .encounterId(encounterId)
                .cie10Code("K59.0")
                .diagnosisType("PRINCIPAL")
                .certainty("CONFIRMED")
                .build();

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDiagnosis_duplicatePrincipal_returns400() throws Exception {
        CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                .encounterId(encounterId)
                .cie10Code("K59.0")
                .diagnosisType("PRINCIPAL")
                .certainty("CONFIRMED")
                .build();

        when(diagnosisService.createDiagnosis(any(CreateDiagnosisRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "A principal diagnosis already exists for this encounter"));

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void listDiagnoses_happyPath_returns200() throws Exception {
        DiagnosisResponse d1 = DiagnosisResponse.builder()
                .id(diagnosisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.0")
                .diagnosisType(Diagnosis.DiagnosisType.PRINCIPAL)
                .certainty(Diagnosis.Certainty.CONFIRMED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DiagnosisResponse d2 = DiagnosisResponse.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("E11.9")
                .diagnosisType(Diagnosis.DiagnosisType.COMORBILIDAD)
                .certainty(Diagnosis.Certainty.CONFIRMED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(diagnosisService.getByEncounter(encounterId)).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$[0].diagnosisType").value("PRINCIPAL"))
                .andExpect(jsonPath("$[1].diagnosisType").value("COMORBILIDAD"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getPrincipal_happyPath_returns200() throws Exception {
        DiagnosisResponse response = DiagnosisResponse.builder()
                .id(diagnosisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.0")
                .diagnosisType(Diagnosis.DiagnosisType.PRINCIPAL)
                .certainty(Diagnosis.Certainty.CONFIRMED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(diagnosisService.getPrincipal(encounterId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/diagnoses/principal", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$.diagnosisType").value("PRINCIPAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getPrincipal_notFound_returns404() throws Exception {
        when(diagnosisService.getPrincipal(encounterId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "No principal diagnosis found for this encounter"));

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/diagnoses/principal", encounterId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void setPrincipal_happyPath_returns200() throws Exception {
        DiagnosisResponse response = DiagnosisResponse.builder()
                .id(diagnosisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.0")
                .diagnosisType(Diagnosis.DiagnosisType.PRINCIPAL)
                .certainty(Diagnosis.Certainty.CONFIRMED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(diagnosisService.setPrincipal(encounterId, diagnosisId)).thenReturn(response);

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/diagnoses/{diagnosisId}/principal", encounterId, diagnosisId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(diagnosisId.toString()))
                .andExpect(jsonPath("$.diagnosisType").value("PRINCIPAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void setPrincipal_wrongEncounter_returns400() throws Exception {
        when(diagnosisService.setPrincipal(encounterId, diagnosisId))
                .thenThrow(new IllegalArgumentException("Diagnosis does not belong to the specified encounter"));

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/diagnoses/{diagnosisId}/principal", encounterId, diagnosisId)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void createDiagnosis_ipsAdminRole_allowed() throws Exception {
        CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                .encounterId(encounterId)
                .cie10Code("K59.0")
                .diagnosisType("PRINCIPAL")
                .certainty("CONFIRMED")
                .build();

        DiagnosisResponse response = DiagnosisResponse.builder()
                .id(diagnosisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(diagnosisService.createDiagnosis(any(CreateDiagnosisRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/diagnoses", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}