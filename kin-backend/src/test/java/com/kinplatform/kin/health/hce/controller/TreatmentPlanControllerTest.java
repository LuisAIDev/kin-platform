package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.TreatmentPlanResponse;
import com.kinplatform.kin.health.hce.dto.request.TreatmentPlanRequest;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.service.TreatmentPlanService;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TreatmentPlanController.class)
@ContextConfiguration(classes = {TreatmentPlanController.class, TreatmentPlanControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class TreatmentPlanControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TreatmentPlanService treatmentPlanService;

    private UUID encounterId;
    private UUID planId;
    private UUID patientId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        planId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createPlan_happyPath_returns201() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("Controlar dolor", "Mejorar función"),
                "GOOD"
        );

        TreatmentPlanResponse response = TreatmentPlanResponse.builder()
                .id(planId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .therapeuticGoals(new String[]{"Controlar dolor", "Mejorar función"})
                .followupPlan("Revisión en 2 semanas")
                .reevaluationCriteria("Escala de dolor < 3")
                .prognosis(TreatmentPlan.Prognosis.GOOD)
                .estimatedDuration(Duration.ofDays(14))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(treatmentPlanService.createPlan(any())).thenReturn(response);

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/treatment-plan", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.conduct").value("OUTPATIENT_TREATMENT"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createPlan_validationError_returns400() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                null,
                null,
                null
        );

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/treatment-plan", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlan_unauthenticated_returns401() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("Controlar dolor"),
                "GOOD"
        );

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/treatment-plan", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createPlan_wrongRole_returns403() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("Controlar dolor"),
                "GOOD"
        );

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/treatment-plan", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void listPlans_happyPath_returns200() throws Exception {
        TreatmentPlanResponse p1 = TreatmentPlanResponse.builder()
                .id(planId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .therapeuticGoals(new String[]{"Controlar dolor"})
                .prognosis(TreatmentPlan.Prognosis.GOOD)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TreatmentPlanResponse p2 = TreatmentPlanResponse.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.REFERRAL)
                .therapeuticGoals(new String[]{"Derivar a especialista"})
                .prognosis(TreatmentPlan.Prognosis.FAIR)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(treatmentPlanService.getByEncounter(encounterId)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/treatment-plan", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId.toString()))
                .andExpect(jsonPath("$[0].conduct").value("OUTPATIENT_TREATMENT"))
                .andExpect(jsonPath("$[1].conduct").value("REFERRAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void updatePlan_happyPath_returns200() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "HOSPITALIZATION",
                List.of("Estabilizar paciente"),
                "FAIR"
        );

        TreatmentPlanResponse response = TreatmentPlanResponse.builder()
                .id(planId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.HOSPITALIZATION)
                .therapeuticGoals(new String[]{"Estabilizar paciente"})
                .prognosis(TreatmentPlan.Prognosis.FAIR)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(treatmentPlanService.updatePlan(eq(planId), any())).thenReturn(response);

        mockMvc.perform(put("/health/hce/treatment-plans/{planId}", planId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.conduct").value("HOSPITALIZATION"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void updatePlan_notFound_returns404() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "HOSPITALIZATION",
                List.of("Estabilizar paciente"),
                "FAIR"
        );

        when(treatmentPlanService.updatePlan(eq(planId), any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Treatment plan not found"));

        mockMvc.perform(put("/health/hce/treatment-plans/{planId}", planId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void createPlan_ipsAdminRole_allowed() throws Exception {
        TreatmentPlanRequest request = new TreatmentPlanRequest(
                "OUTPATIENT_TREATMENT",
                List.of("Controlar dolor"),
                "GOOD"
        );

        TreatmentPlanResponse response = TreatmentPlanResponse.builder()
                .id(planId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(treatmentPlanService.createPlan(any())).thenReturn(response);

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/treatment-plan", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}