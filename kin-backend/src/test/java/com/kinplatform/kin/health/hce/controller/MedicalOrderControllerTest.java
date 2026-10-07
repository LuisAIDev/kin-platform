package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.hce.dto.CancelOrderRequest;
import com.kinplatform.kin.health.hce.dto.CreateMedicalOrderRequest;
import com.kinplatform.kin.health.hce.dto.MedicalOrderResponse;
import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.service.MedicalOrderService;
import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.common.user.User;
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

@WebMvcTest(controllers = MedicalOrderController.class)
@ContextConfiguration(classes = {MedicalOrderController.class, MedicalOrderControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class MedicalOrderControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicalOrderService medicalOrderService;

    @MockBean
    private UserRepository userRepository;

    private UUID encounterId;
    private UUID orderId;
    private UUID treatmentPlanId;
    private UUID patientId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        treatmentPlanId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    private CreateMedicalOrderRequest validRequest() {
        return CreateMedicalOrderRequest.builder()
                .treatmentPlanId(treatmentPlanId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.ROUTINE)
                .cupsCode("890301")
                .build();
    }

    private MedicalOrderResponse sampleResponse() {
        return MedicalOrderResponse.builder()
                .id(orderId)
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.ROUTINE)
                .status(MedicalOrder.Status.ORDERED)
                .orderedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createOrder_happyPath_returns201() throws Exception {
        when(medicalOrderService.addOrderForEncounter(eq(encounterId), any(CreateMedicalOrderRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/orders", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.encounterId").value(encounterId.toString()))
                .andExpect(jsonPath("$.orderType").value("LAB_EXAM"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void createOrder_validationError_returns400() throws Exception {
        CreateMedicalOrderRequest invalid = CreateMedicalOrderRequest.builder().build();

        mockMvc.perform(post("/health/hce/encounters/{encounterId}/orders", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/orders", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void createOrder_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/health/hce/encounters/{encounterId}/orders", encounterId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "physician@test.com", roles = "PHYSICIAN")
    void executeOrder_happyPath_returns200() throws Exception {
        User physician = User.builder().id(physicianId).email("physician@test.com").build();
        when(userRepository.findByEmail("physician@test.com")).thenReturn(Optional.of(physician));
        when(medicalOrderService.executeOrder(orderId, physicianId)).thenReturn(sampleResponse());

        mockMvc.perform(put("/health/hce/orders/{orderId}/execute", orderId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void cancelOrder_happyPath_returns200() throws Exception {
        CancelOrderRequest request = CancelOrderRequest.builder().reason("Patient refused").build();
        when(medicalOrderService.cancelOrder(orderId, "Patient refused")).thenReturn(sampleResponse());

        mockMvc.perform(put("/health/hce/orders/{orderId}/cancel", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void cancelOrder_blankReason_returns400() throws Exception {
        CancelOrderRequest request = CancelOrderRequest.builder().reason("").build();

        mockMvc.perform(put("/health/hce/orders/{orderId}/cancel", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByEncounter_happyPath_returns200() throws Exception {
        when(medicalOrderService.getByEncounter(encounterId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/health/hce/encounters/{encounterId}/orders", encounterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderType").value("LAB_EXAM"));
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void getByTreatmentPlan_happyPath_returns200() throws Exception {
        when(medicalOrderService.getByTreatmentPlan(treatmentPlanId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/health/hce/treatment-plans/{planId}/orders", treatmentPlanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }
}

