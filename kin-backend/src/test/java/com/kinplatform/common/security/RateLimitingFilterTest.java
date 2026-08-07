package com.kinplatform.common.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new RateLimitingFilter();
        setField(filter, "rateLimitEnabled", true);
        setField(filter, "trustProxyHeaders", false);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = RateLimitingFilter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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
    void rutaAuth_dentroDelLimite_deberiaPasar() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        for (int i = 0; i < 5; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void rutaAuth_excedida_deberiaDevolver429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        for (int i = 0; i < 6; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }

    @Test
    void desactivado_noDeberiaLimitar() throws Exception {
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", false);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response, never()).setStatus(429);
    }

    @Test
    void trustProxyHeaders_confiaEnXFF() throws Exception {
        ReflectionTestUtils.setField(filter, "trustProxyHeaders", true);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.9, 10.0.0.5");
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // El XFF (203.0.113.9) se usa como clave de bucket
        for (int i = 0; i < 6; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }

    @Test
    void sinTrustProxy_ignoraXFF() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getContextPath()).thenReturn("/api/v1");
        when(request.getRemoteAddr()).thenReturn("10.0.0.7");
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // 5 requests con la misma remoteAddr pasan; la 6 entrega 429 aunque XFF rote
        for (int i = 0; i < 6; i++) {
            filter.doFilter(request, response, filterChain);
        }

        verify(response).setStatus(429);
    }
}
