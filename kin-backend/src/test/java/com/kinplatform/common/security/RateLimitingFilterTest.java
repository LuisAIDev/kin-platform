package com.kinplatform.common.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitProperties properties;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setTrustProxyHeaders(false);
        filter = new RateLimitingFilter(properties);
    }

    private void loginUri() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getContextPath()).thenReturn("/api/v1");
    }

    @Test
    void rutaNoAuth_noDeberiaLimitarse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getContextPath()).thenReturn("/api/v1");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void login_dentroDelLimite_deberiaPasar() throws Exception {
        loginUri();
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void login_excedido_deberiaDevolver429() throws Exception {
        loginUri();
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }

    @Test
    void resendVerification_limiteInferior_deberiaDevolver429() throws Exception {
        // /auth/resend-verification tiene límite 5/min (más estricto que /auth/login).
        when(request.getRequestURI()).thenReturn("/api/v1/auth/resend-verification");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        for (int i = 0; i < 6; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }

    @Test
    void prefijoMasEspecifico_ganaAlGenerico() throws Exception {
        // /auth/login usa su bucket propio (10/min) y NO el genérico /auth/.
        loginUri();
        when(request.getRemoteAddr()).thenReturn("10.0.0.4");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // 11 llamadas de login: las 10 primeras pasan, la 11 devuelve 429.
        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }
        verify(response).setStatus(429);
        verify(filterChain, times(10)).doFilter(request, response);
    }

    @Test
    void whitelist_deberiaEximirDelLimite() throws Exception {
        properties.setWhitelist(List.of("203.0.113.50"));
        loginUri();
        when(request.getRemoteAddr()).thenReturn("203.0.113.50");

        for (int i = 0; i < 30; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(30)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void desactivado_noDeberiaLimitar() throws Exception {
        properties.setEnabled(false);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response, never()).setStatus(429);
    }

    @Test
    void reset_deberiaLimpiarLosBucketsDeLaIP() throws Exception {
        loginUri();
        when(request.getRemoteAddr()).thenReturn("10.0.0.6");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Bloquear la IP.
        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }
        verify(response).setStatus(429);

        // Reset limpia los buckets.
        int cleared = filter.reset("10.0.0.6");
        org.junit.jupiter.api.Assertions.assertTrue(cleared > 0);

        // Tras el reset la IP puede reintentar.
        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(15)).doFilter(request, response);
    }

    @Test
    void trustProxyHeaders_confiaEnXFF() throws Exception {
        properties.setTrustProxyHeaders(true);
        loginUri();
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.9, 10.0.0.5");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }

    @Test
    void sinTrustProxy_ignoraXFF() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.7");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        for (int i = 0; i < 11; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }

    @Test
    void institutionalInquiries_dentroDelLimite_deberiaPasar() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/institutional/inquiries");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");

        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void institutionalInquiries_excedido_deberiaDevolver429() throws Exception {
        // Endpoint publico /institutional/inquiries: limite 5/min por IP.
        when(request.getRequestURI()).thenReturn("/api/v1/institutional/inquiries");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        for (int i = 0; i < 6; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }
}
