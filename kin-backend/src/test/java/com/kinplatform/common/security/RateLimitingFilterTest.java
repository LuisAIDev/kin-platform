package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;

    private static final String AUTH_PATH = "/api/v1/auth/login";
    private static final String NON_AUTH_PATH = "/api/v1/projects";
    private static final String CONTEXT_PATH = "/api/v1";

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    private void setupRequest(String path, String ip) {
        when(request.getContextPath()).thenReturn(CONTEXT_PATH);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getRemoteAddr()).thenReturn(ip);
    }

    @Test
    void doFilter_deberiaPermitirRequest_cuandoNoEsRutaAuth() throws Exception {
        when(request.getContextPath()).thenReturn(CONTEXT_PATH);
        when(request.getRequestURI()).thenReturn(NON_AUTH_PATH);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_deberiaPermitirLosPrimeros5Intentos_dentroDeLaVentana() throws Exception {
        setupRequest(AUTH_PATH, "192.168.1.10");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_deberiaBloquear_enElSextoIntento_conStatus429() throws Exception {
        setupRequest(AUTH_PATH, "192.168.1.10");
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        assertTrue(sw.toString().contains("Demasiadas solicitudes"));
        verify(filterChain, times(5)).doFilter(request, response);
    }

    @Test
    void doFilter_deberiaResetearContador_despuesDeExpirarLaVentana() throws Exception {
        Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        filter.clock = fixed;
        setupRequest(AUTH_PATH, "192.168.1.10");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(429);

        // 6th request — blocked
        StringWriter sw1 = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw1));
        filter.doFilterInternal(request, response, filterChain);
        verify(response, times(1)).setStatus(429);

        // Advance clock past the 1-minute window
        filter.clock = Clock.offset(fixed, Duration.ofMinutes(2));

        // Now request should be allowed (window resets)
        filter.doFilterInternal(request, response, filterChain);

        // Make 4 more requests (5 total after reset)
        for (int i = 0; i < 4; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(5 + 5)).doFilter(request, response);

        // 6th after reset — blocked again
        StringWriter sw2 = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw2));
        filter.doFilterInternal(request, response, filterChain);
        verify(response, times(2)).setStatus(429);
    }

    @Test
    void doFilter_deberiaTratarIPsDiferentes_independientemente() throws Exception {
        setupRequest(AUTH_PATH, "10.0.0.1"); // IP A
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain); // IP A blocked

        // Switch to IP B
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain); // IP B blocked

        verify(response, times(2)).setStatus(429);
        verify(filterChain, times(10)).doFilter(request, response);
    }
}
