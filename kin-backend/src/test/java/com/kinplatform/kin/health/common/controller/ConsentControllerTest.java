package com.kinplatform.kin.health.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.common.dto.CreateUserConsentRequest;
import com.kinplatform.kin.health.common.dto.RevokeConsentRequest;
import com.kinplatform.kin.health.common.dto.UserConsentResponse;
import com.kinplatform.kin.health.common.service.ConsentService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
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

@WebMvcTest(controllers = ConsentController.class)
@ContextConfiguration(classes = {ConsentController.class, ConsentControllerTest.TestSecurityConfig.class})
@Import({GlobalExceptionHandler.class})
class ConsentControllerTest {

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsentService consentService;

    @MockBean
    private UserRepository userRepository;

    private UUID userId;
    private UUID consentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        consentId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of(() -> "ROLE_PATIENT"))
        );
    }

    private CreateUserConsentRequest validRequest() {
        return CreateUserConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();
    }

    private UserConsentResponse sampleResponse() {
        return UserConsentResponse.builder()
                .id(consentId)
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createConsent_happyPath_returns201() throws Exception {
        when(consentService.createOrUpdateConsent(any(CreateUserConsentRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/health/consents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.consentType").value("HEALTH_DATA"))
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    void createConsent_validationError_returns400() throws Exception {
        CreateUserConsentRequest invalid = CreateUserConsentRequest.builder().build();

        mockMvc.perform(post("/health/consents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createConsent_unauthenticated_returns403() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/health/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyConsents_happyPath_returns200() throws Exception {
        UserConsentResponse c1 = UserConsentResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now())
                .build();
        UserConsentResponse c2 = UserConsentResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .consentType("MARKETING")
                .version("1.0")
                .accepted(false)
                .build();

        when(consentService.getUserConsents(userId))
                .thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/health/consents/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consentType").value("HEALTH_DATA"))
                .andExpect(jsonPath("$[1].consentType").value("MARKETING"));
    }

    @Test
    void getActiveConsent_found_returns200() throws Exception {
        UserConsentResponse active = UserConsentResponse.builder()
                .id(consentId)
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(true)
                .acceptedAt(Instant.now())
                .build();

        when(consentService.getActiveConsent(userId, "HEALTH_DATA"))
                .thenReturn(Optional.of(active));

        mockMvc.perform(get("/health/consents/me/HEALTH_DATA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.consentType").value("HEALTH_DATA"))
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    void getActiveConsent_notFound_returns404() throws Exception {
        when(consentService.getActiveConsent(userId, "MARKETING"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/health/consents/me/MARKETING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokeConsent_happyPath_returns200() throws Exception {
        RevokeConsentRequest request = RevokeConsentRequest.builder()
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .build();

        UserConsentResponse revoked = UserConsentResponse.builder()
                .id(consentId)
                .userId(userId)
                .consentType("HEALTH_DATA")
                .version("1.0")
                .accepted(false)
                .revokedAt(Instant.now())
                .build();

        when(consentService.revokeConsent(any(RevokeConsentRequest.class)))
                .thenReturn(revoked);

        mockMvc.perform(post("/health/consents/{id}/revoke", consentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.revokedAt").exists());
    }

    @Test
    void checkConsent_happyPath_returns200() throws Exception {
        when(consentService.hasActiveConsent(userId, "HEALTH_DATA"))
                .thenReturn(true);

        mockMvc.perform(get("/health/consents/check/HEALTH_DATA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
