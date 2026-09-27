package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.AnamnesisResponse;
import com.kinplatform.kin.health.hce.dto.CreateAnamnesisRequest;
import com.kinplatform.kin.health.hce.service.AnamnesisService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnamnesisController.class)
@ContextConfiguration(classes = {AnamnesisController.class, AnamnesisControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class AnamnesisControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnamnesisService anamnesisService;

    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private UUID anamnesisId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        anamnesisId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void upsertAnamnesis_happyPath_returns201() throws Exception {
        CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                .encounterId(encounterId)
                .onsetDatetime(Instant.now())
                .evolutionDescription("Dolor abdominal agudo")
                .severitySelfReported(7)
                .build();

        AnamnesisResponse response = AnamnesisResponse.builder()
                .id(anamnesisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .onsetDatetime(Instant.now())
                .evolutionDescription("Dolor abdominal agudo")
                .severitySelfReported(7)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(anamnesisService.createAnamnesis(any(CreateAnamnesisRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(anamnesisId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.severitySelfReported").value(7));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void upsertAnamnesis_validationError_returns400() throws Exception {
        CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                .encounterId(null)
                .build();

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertAnamnesis_unauthenticated_returns401() throws Exception {
        CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                .encounterId(encounterId)
                .onsetDatetime(Instant.now())
                .evolutionDescription("Dolor abdominal agudo")
                .build();

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void upsertAnamnesis_wrongRole_returns403() throws Exception {
        CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                .encounterId(encounterId)
                .onsetDatetime(Instant.now())
                .evolutionDescription("Dolor abdominal agudo")
                .build();

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getAnamnesis_happyPath_returns200() throws Exception {
        AnamnesisResponse response = AnamnesisResponse.builder()
                .id(anamnesisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .onsetDatetime(Instant.now())
                .evolutionDescription("Dolor abdominal agudo")
                .severitySelfReported(7)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(anamnesisService.getByEncounterId(encounterId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(anamnesisId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getAnamnesis_notFound_returns404() throws Exception {
        when(anamnesisService.getByEncounterId(encounterId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Anamnesis not found for encounter"));

        mockMvc.perform(get("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void upsertAnamnesis_ipsAdminRole_allowed() throws Exception {
        CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                .encounterId(encounterId)
                .onsetDatetime(Instant.now())
                .evolutionDescription("Dolor abdominal agudo")
                .build();

        AnamnesisResponse response = AnamnesisResponse.builder()
                .id(anamnesisId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(anamnesisService.createAnamnesis(any(CreateAnamnesisRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/health/hce/encounters/{encounterId}/anamnesis", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}