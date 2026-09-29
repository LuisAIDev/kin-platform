package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreatePhysicalExamRequest;
import com.kinplatform.kin.health.hce.dto.PhysicalExamResponse;
import com.kinplatform.kin.health.hce.service.PhysicalExamService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PhysicalExamController.class)
@ContextConfiguration(classes = {PhysicalExamController.class, PhysicalExamControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class PhysicalExamControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhysicalExamService physicalExamService;

    private UUID encounterId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void recordExam_happyPath_returns201() throws Exception {
        CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                .encounterId(encounterId)
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .build();

        PhysicalExamResponse response = PhysicalExamResponse.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(physicalExamService.recordExam(any(CreatePhysicalExamRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/health/hce/encounters/{encounterId}/physical-exam", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.bpSystolic").value(120));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void recordExam_validationError_returns400() throws Exception {
        CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                .encounterId(null)
                .bpSystolic(999)
                .build();

        mockMvc.perform(put("/health/hce/encounters/{encounterId}/physical-exam", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordExam_unauthenticated_returns401() throws Exception {
        CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                .encounterId(encounterId)
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .build();

        mockMvc.perform(put("/health/hce/encounters/{encounterId}/physical-exam", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void recordExam_wrongRole_returns403() throws Exception {
        CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                .encounterId(encounterId)
                .bpSystolic(120)
                .build();

        mockMvc.perform(put("/health/hce/encounters/{encounterId}/physical-exam", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_happyPath_returns200() throws Exception {
        PhysicalExamResponse response = PhysicalExamResponse.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(physicalExamService.getByEncounter(encounterId)).thenReturn(response);

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/physical-exam", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_notFound_returns404() throws Exception {
        when(physicalExamService.getByEncounter(encounterId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Physical exam not found"));

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/physical-exam", encounterId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getLatestByPatient_happyPath_returns200() throws Exception {
        PhysicalExamResponse response = PhysicalExamResponse.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(physicalExamService.getLatestByPatient(patientId)).thenReturn(response);

        mockMvc.perform(get("/health/hce/patients/{patientId}/physical-exam/latest", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void getLatestByPatient_ipsAdminRole_allowed() throws Exception {
        PhysicalExamResponse response = PhysicalExamResponse.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .bpSystolic(120)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(physicalExamService.getLatestByPatient(patientId)).thenReturn(response);

        mockMvc.perform(get("/health/hce/patients/{patientId}/physical-exam/latest", patientId))
                .andExpect(status().isOk());
    }
}