package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CreateObstetricHistoryRequest;
import com.kinplatform.kin.health.hce.dto.ObstetricHistoryResponse;
import com.kinplatform.kin.health.hce.service.ObstetricHistoryService;
import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.common.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ObstetricHistoryController.class)
@ContextConfiguration(classes = {ObstetricHistoryController.class, ObstetricHistoryControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class ObstetricHistoryControllerTest {

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObstetricHistoryService obstetricHistoryService;

    @MockBean
    private UserRepository userRepository;

    private UUID patientId;
    private UUID historyId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        historyId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CreateObstetricHistoryRequest validRequest() {
        return CreateObstetricHistoryRequest.builder()
                .patientId(patientId)
                .gravida(3)
                .para(2)
                .abortions(0)
                .ectopicPregnancies(0)
                .stillbirths(0)
                .livingChildren(2)
                .currentPregnancy(false)
                .lmp(LocalDate.now().minusWeeks(12))
                .estimatedEdd(LocalDate.now().plusWeeks(28))
                .gestationalWeeks(12)
                .prenatalControls(3)
                .previousDeliveries("[{\"type\":\"vaginal\",\"year\":2020},{\"type\":\"vaginal\",\"year\":2022}]")
                .breastfeedingStatus("EXCLUSIVE")
                .breastfeedingDurationMonths(6)
                .obstetricComplications("[{\"type\":\"preeclampsia\",\"year\":2022}]")
                .build();
    }

    private ObstetricHistoryResponse sampleResponse() {
        return ObstetricHistoryResponse.builder()
                .id(historyId)
                .patientId(patientId)
                .gravida(3)
                .para(2)
                .abortions(0)
                .ectopicPregnancies(0)
                .stillbirths(0)
                .livingChildren(2)
                .currentPregnancy(false)
                .lmp(LocalDate.now().minusWeeks(12))
                .estimatedEdd(LocalDate.now().plusWeeks(28))
                .gestationalWeeks(12)
                .prenatalControls(3)
                .previousDeliveries("[{\"type\":\"vaginal\",\"year\":2020},{\"type\":\"vaginal\",\"year\":2022}]")
                .breastfeedingStatus("EXCLUSIVE")
                .breastfeedingDurationMonths(6)
                .obstetricComplications("[{\"type\":\"preeclampsia\",\"year\":2022}]")
                .recordedBy(physicianId)
                .recordedAt(java.time.Instant.now())
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void upsertHistory_happyPath_returns201() throws Exception {
        when(obstetricHistoryService.upsertHistory(any(CreateObstetricHistoryRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/hce/patients/{patientId}/obstetric-history", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.gravida").value(3))
                .andExpect(jsonPath("$.para").value(2));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void upsertHistory_validationError_returns400() throws Exception {
        CreateObstetricHistoryRequest invalid = CreateObstetricHistoryRequest.builder().build();

        mockMvc.perform(post("/health/hce/patients/{patientId}/obstetric-history", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertHistory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/patients/{patientId}/obstetric-history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void upsertHistory_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/patients/{patientId}/obstetric-history", patientId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void updateHistory_happyPath_returns200() throws Exception {
        when(obstetricHistoryService.upsertHistory(any(CreateObstetricHistoryRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/health/hce/obstetric-history/{id}", historyId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.gravida").value(3));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByPatientId_happyPath_returns200() throws Exception {
        when(obstetricHistoryService.getByPatientId(patientId))
                .thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/health/hce/patients/{patientId}/obstetric-history", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.gravida").value(3))
                .andExpect(jsonPath("$.currentPregnancy").value(false));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByPatientId_notFound_returns404() throws Exception {
        when(obstetricHistoryService.getByPatientId(patientId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/health/hce/patients/{patientId}/obstetric-history", patientId))
                .andExpect(status().isNotFound());
    }
}
