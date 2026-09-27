package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.dto.UpdateEncounterRequest;
import com.kinplatform.kin.health.hce.service.EncounterService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EncounterController.class)
@ContextConfiguration(classes = {EncounterController.class, EncounterControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class EncounterControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EncounterService encounterService;

    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createEncounter_happyPath_returns201() throws Exception {
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(patientId)
                .encounterType("EMERGENCY")
                .chiefComplaint("Dolor abdominal agudo")
                .appointmentId(null)
                .build();

        EncounterResponse response = EncounterResponse.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType("EMERGENCY")
                .status("IN_PROGRESS")
                .chiefComplaint("Dolor abdominal agudo")
                .startedAt(Instant.now())
                .closedAt(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(encounterService.createEncounter(any(CreateEncounterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/health/hce/encounters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(encounterId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.chiefComplaint").value("Dolor abdominal agudo"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createEncounter_validationError_returns400() throws Exception {
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(null)
                .encounterType("EMERGENCY")
                .chiefComplaint("Dolor abdominal agudo")
                .appointmentId(null)
                .build();

        mockMvc.perform(post("/api/v1/health/hce/encounters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEncounter_unauthenticated_returns401() throws Exception {
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(patientId)
                .encounterType("EMERGENCY")
                .chiefComplaint("Dolor abdominal agudo")
                .appointmentId(null)
                .build();

        mockMvc.perform(post("/api/v1/health/hce/encounters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createEncounter_wrongRole_returns403() throws Exception {
        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(patientId)
                .encounterType("EMERGENCY")
                .chiefComplaint("Dolor abdominal agudo")
                .appointmentId(null)
                .build();

        mockMvc.perform(post("/api/v1/health/hce/encounters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getEncounter_happyPath_returns200() throws Exception {
        EncounterResponse response = EncounterResponse.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType("EMERGENCY")
                .status("IN_PROGRESS")
                .chiefComplaint("Dolor abdominal agudo")
                .startedAt(Instant.now())
                .closedAt(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(encounterService.getEncounter(encounterId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/hce/encounters/{id}", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(encounterId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getEncounter_notFound_returns404() throws Exception {
        when(encounterService.getEncounter(encounterId))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Encounter not found"));

        mockMvc.perform(get("/api/v1/health/hce/encounters/{id}", encounterId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void updateEncounter_happyPath_returns200() throws Exception {
        UpdateEncounterRequest request = UpdateEncounterRequest.builder()
                .chiefComplaint("Dolor abdominal actualizado")
                .encounterType("INPATIENT")
                .build();

        EncounterResponse response = EncounterResponse.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType("INPATIENT")
                .status("IN_PROGRESS")
                .chiefComplaint("Dolor abdominal actualizado")
                .startedAt(Instant.now())
                .closedAt(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(encounterService.updateEncounter(eq(encounterId), any(UpdateEncounterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/health/hce/encounters/{id}", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encounterType").value("INPATIENT"))
                .andExpect(jsonPath("$.chiefComplaint").value("Dolor abdominal actualizado"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void closeEncounter_happyPath_returns200() throws Exception {
        EncounterResponse response = EncounterResponse.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType("EMERGENCY")
                .status("COMPLETED")
                .chiefComplaint("Dolor abdominal agudo")
                .startedAt(Instant.now())
                .closedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(encounterService.closeEncounter(encounterId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/health/hce/encounters/{id}/close", encounterId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.closedAt").exists());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByPatient_happyPath_returns200() throws Exception {
        EncounterResponse response = EncounterResponse.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType("EMERGENCY")
                .status("IN_PROGRESS")
                .chiefComplaint("Dolor abdominal agudo")
                .startedAt(Instant.now())
                .closedAt(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(encounterService.findByPatientId(patientId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/health/hce/encounters")
                        .param("patientId", patientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(encounterId.toString()))
                .andExpect(jsonPath("$[0].patientId").value(patientId.toString()));
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void findByOrganization_happyPath_returns200() throws Exception {
        EncounterResponse response = EncounterResponse.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType("EMERGENCY")
                .status("IN_PROGRESS")
                .chiefComplaint("Dolor abdominal agudo")
                .startedAt(Instant.now())
                .closedAt(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(encounterService.findByOrganizationId(organizationId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/health/hce/encounters")
                        .param("organizationId", organizationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationId").value(organizationId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void findByPatient_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/health/hce/encounters"))
                .andExpect(status().isBadRequest());
    }
}