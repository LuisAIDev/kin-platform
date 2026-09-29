package com.kinplatform.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.entity.PrivacyPolicyVersion;
import com.kinplatform.common.service.PrivacyPolicyService;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PrivacyPolicyController.class)
@Import(com.kinplatform.common.config.SecurityConfig.class)
class PrivacyPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrivacyPolicyService service;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID versionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
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
    void getActivePolicy_noAuth_returns200() throws Exception {
        com.kinplatform.common.entity.PrivacyPolicyVersion active = com.kinplatform.common.entity.PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID())
            .version("1.0")
            .title("Política de Privacidad v1.0")
            .contentMd("# Política v1.0\nContenido...")
            .active(true)
            .build();
        when(service.getActivePolicy()).thenReturn(Optional.of(active));

        mockMvc.perform(get("/api/v1/public/privacy-policy"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("1.0"))
            .andExpect(jsonPath("$.title").value("Política de Privacidad v1.0"));
    }

    @Test
    void getActivePolicy_noActive_returns404() throws Exception {
        when(service.getActivePolicy()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/public/privacy-policy"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllVersions_noAuth_returns200() throws Exception {
        com.kinplatform.common.entity.PrivacyPolicyVersion v1 = com.kinplatform.common.entity.PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID()).version("1.0").title("v1.0").active(true).build();
        com.kinplatform.common.entity.PrivacyPolicyVersion v2 = com.kinplatform.common.entity.PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID()).version("2.0").title("v2.0").active(true).build();
        when(service.listAllVersions()).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/v1/public/privacy-policy/versions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].version").value("1.0"))
            .andExpect(jsonPath("$[1].version").value("2.0"));
    }

    @Test
    void getPolicyByVersion_exists_returns200() throws Exception {
        com.kinplatform.common.entity.PrivacyPolicyVersion version = com.kinplatform.common.entity.PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID())
            .version("1.0")
            .title("Política v1.0")
            .active(true)
            .build();
        when(service.getPolicyByVersion("1.0")).thenReturn(Optional.of(version));

        mockMvc.perform(get("/api/v1/public/privacy-policy/1.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("1.0"));
    }

    @Test
    void getPolicyByVersion_notFound_returns404() throws Exception {
        when(service.getPolicyByVersion("99.0")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/public/privacy-policy/99.0"))
            .andExpect(status().isNotFound());
    }

    @Test
    void publishNewVersion_noAuth_returns403() throws Exception {
        // CSRF filter blocks unauthenticated POST with 403 (Access Denied)
        mockMvc.perform(post("/api/v1/admin/privacy-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2.0\",\"title\":\"v2.0\",\"contentMd\":\"# v2.0\",\"effectiveDate\":\"2025-01-01\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "PATIENT")
    void publishNewVersion_patientRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/privacy-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2.0\",\"title\":\"v2.0\",\"contentMd\":\"# v2.0\",\"effectiveDate\":\"2025-01-01\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "33333333-3333-3333-3333-333333333333", roles = "ADMIN")
    void publishNewVersion_admin_returns201() throws Exception {
        com.kinplatform.common.entity.PrivacyPolicyVersion newVersion = com.kinplatform.common.entity.PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID())
            .version("2.0")
            .title("Política v2.0")
            .contentMd("# v2.0")
            .effectiveDate(java.time.LocalDate.now())
            .active(true)
            .build();
        when(service.publishNewVersion(eq("2.0"), eq("Política v2.0"), eq("# v2.0"), any(), any()))
            .thenReturn(newVersion);

        mockMvc.perform(post("/api/v1/admin/privacy-policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2.0\",\"title\":\"Política v2.0\",\"contentMd\":\"# v2.0\",\"effectiveDate\":\"2025-01-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value("2.0"));
    }
}