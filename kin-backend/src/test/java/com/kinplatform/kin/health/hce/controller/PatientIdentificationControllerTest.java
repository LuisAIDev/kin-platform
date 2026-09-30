package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreatePatientIdentificationRequest;
import com.kinplatform.kin.health.hce.dto.PatientIdentificationResponse;
import com.kinplatform.kin.health.hce.service.PatientIdentificationService;
import com.kinplatform.common.GlobalExceptionHandler;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PatientIdentificationController.class)
@ContextConfiguration(classes = {PatientIdentificationController.class, PatientIdentificationControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class PatientIdentificationControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientIdentificationService service;

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void upsertIdentification_happyPath_returns201() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .rhFactor("O+")
                .regimen("CONTRIBUTIVO")
                .build();

        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .rhFactor("O+")
                .regimen("CONTRIBUTIVO")
                .build();

        when(service.upsertIdentification(eq(patientId), any(CreatePatientIdentificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/health/hce/patients/{patientId}/identification", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andExpect(jsonPath("$.documentNumber").value("1234567890"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void upsertIdentification_validationError_returns400() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreatePatientIdentificationRequest request = new CreatePatientIdentificationRequest();
        request.setUserId(patientId);
        // Missing required fields

        mockMvc.perform(post("/health/hce/patients/{patientId}/identification", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertIdentification_unauthenticated_returns401() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        mockMvc.perform(post("/health/hce/patients/{patientId}/identification", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void upsertIdentification_patientRole_allowed() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        when(service.upsertIdentification(eq(patientId), any(CreatePatientIdentificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/health/hce/patients/{patientId}/identification", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void upsertIdentification_ipsAdminRole_allowed() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        when(service.upsertIdentification(eq(patientId), any(CreatePatientIdentificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/health/hce/patients/{patientId}/identification", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "FREE")
    void upsertIdentification_freeRole_forbidden() throws Exception {
        UUID patientId = UUID.randomUUID();
        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        mockMvc.perform(post("/health/hce/patients/{patientId}/identification", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getIdentification_happyPath_returns200() throws Exception {
        UUID patientId = UUID.randomUUID();
        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(patientId)
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        when(service.getByUserIdOptional(patientId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/health/hce/patients/{patientId}/identification", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(patientId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getIdentification_whenNotExists_returns204() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(service.getByUserIdOptional(patientId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/health/hce/patients/{patientId}/identification", patientId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByDocument_found_returns200() throws Exception {
        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("1234567890")
                .build();

        when(service.findByDocumentNumber("CC", "1234567890")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/health/hce/patients/identification/by-document")
                        .param("documentType", "CC")
                        .param("documentNumber", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andExpect(jsonPath("$.documentNumber").value("1234567890"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByDocument_notFound_returns404() throws Exception {
        when(service.findByDocumentNumber("CC", "9999999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/health/hce/patients/identification/by-document")
                        .param("documentType", "CC")
                        .param("documentNumber", "9999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByEps_happyPath_returns200() throws Exception {
        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("1234567890")
                .epsCode("EPS001")
                .build();

        when(service.findByEpsCode("EPS001")).thenReturn(List.of(response));

        mockMvc.perform(get("/health/hce/patients/identification/by-eps")
                        .param("epsCode", "EPS001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].epsCode").value("EPS001"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByRegimen_happyPath_returns200() throws Exception {
        PatientIdentificationResponse response = PatientIdentificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("1234567890")
                .regimen("CONTRIBUTIVO")
                .build();

        when(service.findByRegimen("CONTRIBUTIVO")).thenReturn(List.of(response));

        mockMvc.perform(get("/health/hce/patients/identification/by-regimen")
                        .param("regimen", "CONTRIBUTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].regimen").value("CONTRIBUTIVO"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByEps_emptyList_returns200() throws Exception {
        when(service.findByEpsCode("NONEXISTENT")).thenReturn(List.of());

        mockMvc.perform(get("/health/hce/patients/identification/by-eps")
                        .param("epsCode", "NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}