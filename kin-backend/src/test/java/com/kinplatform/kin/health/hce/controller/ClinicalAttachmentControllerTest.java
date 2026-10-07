package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.ClinicalAttachmentResponse;
import com.kinplatform.kin.health.hce.dto.CreateClinicalAttachmentRequest;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.service.ClinicalAttachmentService;
import com.kinplatform.common.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(controllers = ClinicalAttachmentController.class)
@ContextConfiguration(classes = {ClinicalAttachmentController.class, ClinicalAttachmentControllerTest.TestSecurityConfig.class})
class ClinicalAttachmentControllerTest {

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClinicalAttachmentService clinicalAttachmentService;

    @MockBean
    private UserRepository userRepository;

    private UUID encounterId;
    private UUID attachmentId;
    private UUID patientId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CreateClinicalAttachmentRequest validRequest() {
        return CreateClinicalAttachmentRequest.builder()
                .encounterId(UUID.randomUUID())
                .attachmentType(ClinicalAttachment.AttachmentType.LAB_RESULT)
                .loincCode("718-7")
                .loincDisplay("Hemoglobin")
                .performedAt(Instant.now())
                .resultValue(new BigDecimal("14.5"))
                .resultUnit("g/dL")
                .abnormalFlag(ClinicalAttachment.AbnormalFlag.NORMAL)
                .build();
    }

    private ClinicalAttachmentResponse sampleResponse() {
        return ClinicalAttachmentResponse.builder()
                .id(UUID.randomUUID())
                .encounterId(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .attachmentType(ClinicalAttachment.AttachmentType.LAB_RESULT)
                .loincCode("718-7")
                .loincDisplay("Hemoglobin")
                .resultValue(new BigDecimal("14.5"))
                .resultUnit("g/dL")
                .abnormalFlag(ClinicalAttachment.AbnormalFlag.NORMAL)
                .performedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void uploadAttachment_happyPath_returns201() throws Exception {
        ClinicalAttachmentResponse response = sampleResponse();

        when(clinicalAttachmentService.uploadAttachment(any(CreateClinicalAttachmentRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/attachments", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.attachmentType").value("LAB_RESULT"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void uploadAttachment_validationError_returns400() throws Exception {
        CreateClinicalAttachmentRequest invalid = CreateClinicalAttachmentRequest.builder().build();

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/attachments", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAttachment_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/attachments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void uploadAttachment_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/attachments", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_happyPath_returns200() throws Exception {
        ClinicalAttachmentResponse a2 = ClinicalAttachmentResponse.builder()
                .id(UUID.randomUUID())
                .attachmentType(ClinicalAttachment.AttachmentType.IMAGING)
                .loincCode("24626-5")
                .performedAt(Instant.now().minusSeconds(7200))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(clinicalAttachmentService.getByEncounter(any(UUID.class)))
                .thenReturn(List.of(sampleResponse(), ClinicalAttachmentResponse.builder()
                        .id(UUID.randomUUID())
                        .attachmentType(ClinicalAttachment.AttachmentType.IMAGING)
                        .loincCode("24626-5")
                        .performedAt(Instant.now().minusSeconds(7200))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/attachments", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attachmentType").value("LAB_RESULT"))
                .andExpect(jsonPath("$[1].attachmentType").value("IMAGING"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_emptyList_returns200() throws Exception {
        when(clinicalAttachmentService.getByEncounter(any(UUID.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/attachments", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByPatientAndType_happyPath_returns200() throws Exception {
        UUID patientId = UUID.randomUUID();
        ClinicalAttachmentResponse a1 = ClinicalAttachmentResponse.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attachmentType(ClinicalAttachment.AttachmentType.LAB_RESULT)
                .performedAt(Instant.now())
                .build();

        ClinicalAttachmentResponse a2 = ClinicalAttachmentResponse.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attachmentType(ClinicalAttachment.AttachmentType.IMAGING)
                .performedAt(Instant.now().minusSeconds(3600))
                .build();

        when(clinicalAttachmentService.getByPatientAndType(patientId, ClinicalAttachment.AttachmentType.LAB_RESULT))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/health/hce/patients/{patientId}/attachments", patientId)
                        .param("type", "LAB_RESULT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attachmentType").value("LAB_RESULT"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByPatient_happyPath_returns200() throws Exception {
        UUID patientId = UUID.randomUUID();
        ClinicalAttachmentResponse a1 = ClinicalAttachmentResponse.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attachmentType(ClinicalAttachment.AttachmentType.LAB_RESULT)
                .performedAt(Instant.now())
                .build();

        ClinicalAttachmentResponse a2 = ClinicalAttachmentResponse.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attachmentType(ClinicalAttachment.AttachmentType.IMAGING)
                .performedAt(Instant.now().minusSeconds(3600))
                .build();

        when(clinicalAttachmentService.getByPatient(patientId))
                .thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/health/hce/patients/{patientId}/attachments", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attachmentType").value("LAB_RESULT"))
                .andExpect(jsonPath("$[1].attachmentType").value("IMAGING"));
    }
}
