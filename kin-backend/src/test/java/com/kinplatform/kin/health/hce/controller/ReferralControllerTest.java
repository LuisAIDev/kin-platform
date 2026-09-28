package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CounterReferralRequest;
import com.kinplatform.kin.health.hce.dto.CreateReferralRequest;
import com.kinplatform.kin.health.hce.dto.ReferralResponse;
import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.service.ReferralService;
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

@WebMvcTest(controllers = ReferralController.class)
@ContextConfiguration(classes = {ReferralController.class, ReferralControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class ReferralControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReferralService referralService;

    private UUID encounterId;
    private UUID referralId;
    private UUID patientId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        referralId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CreateReferralRequest validRequest() {
        return CreateReferralRequest.builder()
                .patientId(patientId)
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referredToInstitution("Hospital Universitario")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.ROUTINE)
                .reason("Paciente requiere evaluación por especialista")
                .build();
    }

    private ReferralResponse sampleResponse() {
        return ReferralResponse.builder()
                .id(referralId)
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Medicina General")
                .referredToService("Neurología")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.ROUTINE)
                .reason("Paciente requiere evaluación por especialista")
                .status(Referral.Status.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createReferral_happyPath_returns201() throws Exception {
        when(referralService.createReferralForEncounter(eq(encounterId), any(CreateReferralRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/referrals", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(referralId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.referralType").value("INTERCONSULTATION"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createReferral_validationError_returns400() throws Exception {
        CreateReferralRequest invalid = CreateReferralRequest.builder().build();

        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/referrals", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReferral_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/referrals", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createReferral_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/health/hce/encounters/{encounterId}/referrals", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void counterReferral_happyPath_returns200() throws Exception {
        CounterReferralRequest request = CounterReferralRequest.builder()
                .counterreferralBy(physicianId)
                .counterreferralSummary("Evaluado, sin hallazgos")
                .build();
        when(referralService.counterReferral(eq(referralId), any(CounterReferralRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/health/hce/referrals/{referralId}/counter-referral", referralId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(referralId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByPatient_happyPath_returns200() throws Exception {
        when(referralService.getByPatient(patientId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/health/hce/patients/{patientId}/referrals", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referredToService").value("Neurología"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByStatus_happyPath_returns200() throws Exception {
        when(referralService.getByStatus(Referral.Status.PENDING)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/health/hce/referrals")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
