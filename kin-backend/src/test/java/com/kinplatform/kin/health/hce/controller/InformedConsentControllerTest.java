package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreateInformedConsentRequest;
import com.kinplatform.kin.health.hce.dto.InformedConsentResponse;
import com.kinplatform.kin.health.hce.dto.RevokeConsentRequest;
import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.kin.health.hce.service.InformedConsentService;
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

@WebMvcTest(controllers = InformedConsentController.class)
@ContextConfiguration(classes = {InformedConsentController.class, InformedConsentControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class InformedConsentControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InformedConsentService informedConsentService;

    private UUID encounterId;
    private UUID consentId;
    private UUID patientId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        consentId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CreateInformedConsentRequest validRequest() {
        return CreateInformedConsentRequest.builder()
                .patientId(patientId)
                .procedureName("Apendicectomía laparoscópica")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .documentVersion("v1.2")
                .build();
    }

    private InformedConsentResponse sampleResponse() {
        return InformedConsentResponse.builder()
                .id(consentId)
                .patientId(patientId)
                .procedureName("Apendicectomía laparoscópica")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .documentVersion("v1.2")
                .physicianId(physicianId)
                .status(InformedConsent.Status.VALID)
                .signedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createConsent_happyPath_returns201() throws Exception {
        when(informedConsentService.createConsentForEncounter(eq(encounterId), any(CreateInformedConsentRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/consents", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(consentId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.consentType").value("SURGICAL"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createConsent_validationError_returns400() throws Exception {
        CreateInformedConsentRequest invalid = CreateInformedConsentRequest.builder().build();

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/consents", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createConsent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/consents", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createConsent_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/consents", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void revokeConsent_happyPath_returns200() throws Exception {
        RevokeConsentRequest request = RevokeConsentRequest.builder().reason("Patient changed mind").build();
        when(informedConsentService.revokeConsent(consentId, "Patient changed mind")).thenReturn(sampleResponse());

        mockMvc.perform(put("/health/hce/consents/{consentId}/revoke", consentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(consentId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void revokeConsent_blankReason_returns400() throws Exception {
        RevokeConsentRequest request = RevokeConsentRequest.builder().reason("").build();

        mockMvc.perform(put("/health/hce/consents/{consentId}/revoke", consentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByPatient_happyPath_returns200() throws Exception {
        when(informedConsentService.getByPatient(patientId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/health/hce/patients/{patientId}/consents", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consentType").value("SURGICAL"));
    }
}
