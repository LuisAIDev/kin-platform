package com.kinplatform.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.entity.DataRectificationRequest;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import com.kinplatform.common.service.DataRectificationService;
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

@WebMvcTest(controllers = DataRectificationController.class)
@Import(com.kinplatform.common.config.SecurityConfig.class)
class DataRectificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataRectificationService service;

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
    void requestRectification_happyPath_returns201() throws Exception {
        DataRectificationRequest request = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("+573001111111")
            .newValue("+573002222222")
            .reason("Cambio de número")
            .status("PENDING")
            .build();
        when(service.requestRectification(eq(userId), eq("users.phone"), eq("+573001111111"), eq("+573002222222"), eq("Cambio de número")))
            .thenReturn(request);

        mockMvc.perform(post("/health/data-rectification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RectificationRequestDto("users.phone", "+573001111111", "+573002222222", "Cambio de número"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.fieldPath").value("users.phone"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void requestRectification_noAuth_returns403() throws Exception {
        // CSRF filter blocks unauthenticated POST with 403 (Access Denied)
        mockMvc.perform(post("/health/data-rectification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RectificationRequestDto("users.phone", "old", "new", "reason"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void requestRectification_adminRole_returns403() throws Exception {
        mockMvc.perform(post("/health/data-rectification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RectificationRequestDto("users.phone", "old", "new", "reason"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void getMyRequests_returns200() throws Exception {
        DataRectificationRequest req1 = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("old")
            .newValue("new")
            .reason("reason")
            .status("PENDING")
            .build();
        when(service.getRequestsByUser(userId)).thenReturn(List.of(req1));

        mockMvc.perform(get("/health/data-rectification/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(requestId.toString()));
    }

    @Test
    void getAllRequests_noAuth_returns403() throws Exception {
        // CSRF filter blocks unauthenticated GET with 403 (Access Denied)
        mockMvc.perform(get("/health/data-rectification"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void getAllRequests_patientRole_returns403() throws Exception {
        mockMvc.perform(get("/health/data-rectification"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void getAllRequests_admin_returns200() throws Exception {
        DataRectificationRequest req1 = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("old")
            .newValue("new")
            .reason("reason")
            .status("PENDING")
            .build();
        when(service.getRequestsByStatus("PENDING")).thenReturn(List.of(req1));

        mockMvc.perform(get("/health/data-rectification"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void approveRectification_admin_returns200() throws Exception {
        DataRectificationRequest approved = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("old")
            .newValue("new")
            .reason("reason")
            .status("APPROVED")
            .build();
        when(service.approveRectification(requestId, adminId, "Aprobado")).thenReturn(approved);

        mockMvc.perform(put("/health/data-rectification/{id}/approve", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApproveRequestDto("Aprobado"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void approveRectification_patient_returns403() throws Exception {
        mockMvc.perform(put("/health/data-rectification/{id}/approve", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApproveRequestDto("Aprobado"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void rejectRectification_admin_returns200() throws Exception {
        DataRectificationRequest rejected = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("old")
            .newValue("new")
            .reason("reason")
            .status("REJECTED")
            .build();
        when(service.rejectRectification(requestId, adminId, "Datos inválidos")).thenReturn(rejected);

        mockMvc.perform(put("/health/data-rectification/{id}/reject", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RejectRequestDto("Datos inválidos"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    record RectificationRequestDto(String fieldPath, String oldValue, String newValue, String reason) {}
    record ApproveRequestDto(String notes) {}
    record RejectRequestDto(String reason) {}
}