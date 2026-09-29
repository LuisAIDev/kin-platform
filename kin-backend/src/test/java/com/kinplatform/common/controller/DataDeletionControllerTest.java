package com.kinplatform.common.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.kinplatform.common.entity.DataDeletionRequest;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import com.kinplatform.common.service.DataDeletionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DataDeletionController.class)
@Import(com.kinplatform.common.config.SecurityConfig.class)
class DataDeletionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataDeletionService service;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID requestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID adminId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() throws Exception {
        passThrough(jwtAuthenticationFilter);
        passThrough(rateLimitingFilter);
        passThrough(subscriptionAccessFilter);
    }

    private void passThrough(jakarta.servlet.Filter filter) throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void requestDeletion_happyPath_returns201() throws Exception {
        DataDeletionRequest request = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Eliminar datos")
            .scope("FULL")
            .status("PENDING")
            .legalHold(false)
            .build();
        when(service.requestDeletion(eq(userId), eq("Eliminar datos"), eq("FULL"), isNull()))
            .thenReturn(request);

        mockMvc.perform(post("/api/v1/health/data-deletion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeletionRequestDto("Eliminar datos", "FULL", null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void requestDeletion_noAuth_returns403() throws Exception {
        // CSRF filter blocks unauthenticated POST with 403 (Access Denied)
        mockMvc.perform(post("/api/v1/health/data-deletion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeletionRequestDto("razón", "FULL", null))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void requestDeletion_adminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/health/data-deletion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeletionRequestDto("razón", "FULL", null))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void getMyRequests_returns200() throws Exception {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Eliminar")
            .scope("FULL")
            .status("PENDING")
            .build();
        when(service.getRequestsByUser(userId)).thenReturn(List.of(req));

        mockMvc.perform(get("/api/v1/health/data-deletion/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(requestId.toString()));
    }

    @Test
    void getAllRequests_noAuth_returns403() throws Exception {
        // CSRF filter blocks unauthenticated GET with 403 (Access Denied)
        mockMvc.perform(get("/api/v1/health/data-deletion"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void getAllRequests_patientRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/health/data-deletion"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void getAllRequests_admin_returns200() throws Exception {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Eliminar")
            .scope("FULL")
            .status("PENDING")
            .build();
        when(service.getRequestsByStatus("PENDING")).thenReturn(List.of(req));

        mockMvc.perform(get("/api/v1/health/data-deletion"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void approveDeletion_admin_returns200() throws Exception {
        DataDeletionRequest approved = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Eliminar")
            .scope("FULL")
            .status("APPROVED")
            .build();
        when(service.approveDeletion(requestId, adminId)).thenReturn(approved);

        mockMvc.perform(put("/api/v1/health/data-deletion/{id}/approve", requestId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void approveDeletion_patient_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/health/data-deletion/{id}/approve", requestId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void rejectDeletion_admin_returns200() throws Exception {
        DataDeletionRequest rejected = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Eliminar")
            .scope("FULL")
            .status("REJECTED")
            .build();
        when(service.rejectDeletion(requestId, adminId, "Datos inválidos")).thenReturn(rejected);

        mockMvc.perform(put("/api/v1/health/data-deletion/{id}/reject", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RejectRequestDto("Datos inválidos"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void executeDeletion_withoutConfirm_returns400() throws Exception {
        when(service.executeDeletion(requestId, false))
            .thenThrow(new IllegalArgumentException("Confirmación requerida: confirm=true"));

        mockMvc.perform(post("/api/v1/health/data-deletion/{id}/execute", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ExecuteRequestDto(false))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void executeDeletion_withConfirm_returns200() throws Exception {
        DataDeletionRequest executed = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Eliminar")
            .scope("FULL")
            .status("EXECUTED")
            .build();
        when(service.executeDeletion(requestId, true)).thenReturn(executed);

        mockMvc.perform(post("/api/v1/health/data-deletion/{id}/execute", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ExecuteRequestDto(true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("EXECUTED"));
    }

    record DeletionRequestDto(String reason, String scope, com.fasterxml.jackson.databind.JsonNode dataCategories) {}
    record RejectRequestDto(String reason) {}
    record ExecuteRequestDto(boolean confirm) {}
}