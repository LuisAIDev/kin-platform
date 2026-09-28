package com.kinplatform.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.entity.DataExportRequest;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import com.kinplatform.common.service.DataExportService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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

@WebMvcTest(controllers = DataExportController.class)
@Import(com.kinplatform.common.config.SecurityConfig.class)
class DataExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataExportService service;

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

    @BeforeEach
    void setUp() throws Exception {
        // Use a request attribute to prevent recursive filter invocation
        String filterExecutedAttr = "FILTER_ALREADY_EXECUTED";

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0, HttpServletRequest.class);
            if (request.getAttribute(filterExecutedAttr + "_jwt") != null) {
                return null; // Already executed for this request
            }
            request.setAttribute(filterExecutedAttr + "_jwt", true);
            FilterChain chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0, HttpServletRequest.class);
            if (request.getAttribute(filterExecutedAttr + "_rate") != null) {
                return null;
            }
            request.setAttribute(filterExecutedAttr + "_rate", true);
            FilterChain chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0, HttpServletRequest.class);
            if (request.getAttribute(filterExecutedAttr + "_sub") != null) {
                return null;
            }
            request.setAttribute(filterExecutedAttr + "_sub", true);
            FilterChain chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(subscriptionAccessFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void requestExport_happyPath_returns202() throws Exception {
        DataExportRequest request = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("PENDING")
            .requestedAt(Instant.now())
            .build();
        when(service.requestExport(userId)).thenReturn(request);

        mockMvc.perform(post("/api/v1/health/data-export"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void requestExport_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/health/data-export"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "ANONYMOUS")
    void requestExport_wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/health/data-export"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void listExports_returns200() throws Exception {
        DataExportRequest req1 = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .build();
        DataExportRequest req2 = DataExportRequest.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .status("PENDING")
            .requestedAt(Instant.now())
            .build();
        when(service.getExportsByUser(userId)).thenReturn(List.of(req1, req2));

        mockMvc.perform(get("/api/v1/health/data-export"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(requestId.toString()));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void getStatus_ownRequest_returns200() throws Exception {
        DataExportRequest request = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .build();
        when(service.getExportStatus(requestId, userId)).thenReturn(request);

        mockMvc.perform(get("/api/v1/health/data-export/{id}/status", requestId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId.toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void getStatus_otherUserRequest_returns403() throws Exception {
        when(service.getExportStatus(requestId, userId))
            .thenThrow(new SecurityException("No autorizado"));

        mockMvc.perform(get("/api/v1/health/data-export/{id}/status", requestId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void download_ownRequest_returns200WithBytes() throws Exception {
        byte[] zipBytes = "test zip content".getBytes();
        when(service.downloadExport(requestId, userId)).thenReturn(zipBytes);

        mockMvc.perform(get("/api/v1/health/data-export/{id}/download", requestId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"data-export-" + requestId + ".zip\""))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().bytes(zipBytes));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void download_otherUserRequest_returns403() throws Exception {
        when(service.downloadExport(requestId, userId))
            .thenThrow(new SecurityException("No autorizado"));

        mockMvc.perform(get("/api/v1/health/data-export/{id}/download", requestId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void download_nonExistent_returns404() throws Exception {
        when(service.downloadExport(requestId, userId))
            .thenThrow(new IllegalArgumentException("Export no encontrado"));

        mockMvc.perform(get("/api/v1/health/data-export/{id}/download", requestId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void download_pending_returns400() throws Exception {
        when(service.downloadExport(requestId, userId))
            .thenThrow(new IllegalStateException("Export no completado"));

        mockMvc.perform(get("/api/v1/health/data-export/{id}/download", requestId))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "PATIENT")
    void download_expired_returns400() throws Exception {
        when(service.downloadExport(requestId, userId))
            .thenThrow(new IllegalStateException("Export expirado"));

        mockMvc.perform(get("/api/v1/health/data-export/{id}/download", requestId))
            .andExpect(status().isBadRequest());
    }
}