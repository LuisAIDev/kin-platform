package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreateDischargeSummaryRequest;
import com.kinplatform.kin.health.hce.dto.DischargeSummaryResponse;
import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.kin.health.hce.service.DischargeSummaryService;
import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DischargeSummaryController.class)
@ContextConfiguration(classes = {DischargeSummaryController.class, DischargeSummaryControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class DischargeSummaryControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DischargeSummaryService dischargeSummaryService;

    @MockBean
    private UserRepository userRepository;

    private UUID encounterId;
    private UUID dischargeSummaryId;
    private UUID patientId;
    private UUID physicianId;
    private UUID admissionId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        dischargeSummaryId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        admissionId = UUID.randomUUID();
    }

    private CreateDischargeSummaryRequest validRequest() {
        return CreateDischargeSummaryRequest.builder()
                .encounterId(UUID.randomUUID()) // will be overridden by controller
                .admissionId(UUID.randomUUID())
                .admissionDate(Instant.now().minusSeconds(3600))
                .dischargeDate(Instant.now())
                .admissionDiagnosisCie10("K59.0")
                .dischargeDiagnosisCie10("K59.0")
                .clinicalSummary("Paciente con estreñimiento agudo resuelto")
                .dischargeCondition(DischargeSummary.DischargeCondition.IMPROVED)
                .dischargeDisposition("Domicilio")
                .generalRecommendations("Dieta rica en fibra, hidratación")
                .build();
    }

    private DischargeSummaryResponse sampleResponse() {
        return DischargeSummaryResponse.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .admissionId(UUID.randomUUID())
                .attendingPhysicianId(UUID.randomUUID())
                .admissionDate(Instant.now().minusSeconds(3600))
                .dischargeDate(Instant.now())
                .lengthOfStay(java.time.Duration.ofHours(1))
                .admissionDiagnosisCie10("K59.0")
                .dischargeDiagnosisCie10("K59.0")
                .dischargeCondition(DischargeSummary.DischargeCondition.IMPROVED)
                .dischargeDisposition("Domicilio")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDischargeSummary_happyPath_returns201() throws Exception {
        DischargeSummaryResponse response = sampleResponse();
        when(dischargeSummaryService.createDischargeSummary(any(CreateDischargeSummaryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/discharge", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.dischargeDiagnosisCie10").value("K59.0"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createDischargeSummary_validationError_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/discharge", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDischargeSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/discharge", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createDischargeSummary_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/discharge", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_happyPath_returns200() throws Exception {
        when(dischargeSummaryService.getByEncounter(any(UUID.class)))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/discharge", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dischargeDiagnosisCie10").value("K59.0"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_notFound_returns404() throws Exception {
        when(dischargeSummaryService.getByEncounter(any(UUID.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/discharge", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "physician@test.com", roles = "PHYSICIAN")
    void signDischargeSummary_happyPath_returns200() throws Exception {
        UUID summaryId = UUID.randomUUID();
        UUID physicianId = UUID.randomUUID();
        User physician = User.builder().id(physicianId).email("physician@test.com").build();

        when(userRepository.findByEmail("physician@test.com")).thenReturn(Optional.of(physician));
        when(dischargeSummaryService.signDischargeSummary(any(UUID.class), any(UUID.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/health/hce/discharge-summaries/{summaryId}/sign", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void signDischargeSummary_notFound_returns404() throws Exception {
        when(dischargeSummaryService.signDischargeSummary(any(UUID.class), any(UUID.class)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Discharge summary not found"));

        mockMvc.perform(put("/api/v1/health/hce/discharge-summaries/{summaryId}/sign", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void signDischargeSummary_alreadySigned_returns400() throws Exception {
        when(dischargeSummaryService.signDischargeSummary(any(UUID.class), any(UUID.class)))
                .thenThrow(new IllegalStateException("Discharge summary already signed"));

        mockMvc.perform(put("/api/v1/health/hce/discharge-summaries/{summaryId}/sign", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByAdmission_happyPath_returns200() throws Exception {
        when(dischargeSummaryService.getByAdmission(any(UUID.class)))
                .thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/health/hce/admissions/{admissionId}/discharge", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dischargeDiagnosisCie10").value("K59.0"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByAdmission_notFound_returns404() throws Exception {
        when(dischargeSummaryService.getByAdmission(any(UUID.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/health/hce/admissions/{admissionId}/discharge", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}