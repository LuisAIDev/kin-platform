package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinHeaderYsinCookie_deberiaPasarSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/projects");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void headerSinBearer_deberiaPasarSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc");
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(request.getRequestURI()).thenReturn("/api/v1/projects");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenInvalido_deberiaPasarSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtService.isTokenValid("invalid")).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/api/v1/projects");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValido_deberiaAutenticarConRol() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");
        when(jwtService.isTokenValid("valid")).thenReturn(true);
        when(jwtService.extractEmail("valid")).thenReturn("a@kin.com");
        when(jwtService.extractRole("valid")).thenReturn("ADMIN");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("a@kin.com", auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void tokenEnCookie_deberiaAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("kin_token_v2", "cookie-token")});
        when(jwtService.isTokenValid("cookie-token")).thenReturn(true);
        when(jwtService.extractEmail("cookie-token")).thenReturn("cookie@kin.com");
        when(jwtService.extractRole("cookie-token")).thenReturn("FREE");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("cookie@kin.com", auth.getName());
    }

    @Test
    void tokenInvalidoEnCookie_deberiaPasarSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("kin_token_v2", "bad")});
        when(jwtService.isTokenValid("bad")).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/api/v1/projects");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
