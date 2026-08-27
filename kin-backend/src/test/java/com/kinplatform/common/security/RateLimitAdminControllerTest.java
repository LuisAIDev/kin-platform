package com.kinplatform.common.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RateLimitAdminControllerTest {

    @Mock
    private RateLimitingFilter rateLimitingFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RateLimitAdminController(rateLimitingFilter))
                .build();
    }

    @Test
    void reset_conIpParam_deberiaLimpiarEsaIp() throws Exception {
        when(rateLimitingFilter.reset("203.0.113.9")).thenReturn(2);

        mockMvc.perform(post("/admin/security/rate-limit/reset").param("ip", "203.0.113.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("203.0.113.9"))
                .andExpect(jsonPath("$.cleared").value(2));

        verify(rateLimitingFilter).reset("203.0.113.9");
    }

    @Test
    void reset_sinIp_deberiaUsarLaIpDeLaSolicitud() throws Exception {
        when(rateLimitingFilter.resolveClientIp(org.mockito.ArgumentMatchers.any(HttpServletRequest.class)))
                .thenReturn("198.51.100.7");
        when(rateLimitingFilter.reset("198.51.100.7")).thenReturn(1);

        mockMvc.perform(post("/admin/security/rate-limit/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("198.51.100.7"))
                .andExpect(jsonPath("$.cleared").value(1));

        verify(rateLimitingFilter).reset("198.51.100.7");
    }
}
