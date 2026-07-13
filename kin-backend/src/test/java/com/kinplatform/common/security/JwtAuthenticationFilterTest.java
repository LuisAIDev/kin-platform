package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private static final String VALID_TOKEN = "valid-jwt-token";
    private static final String INVALID_TOKEN = "invalid-jwt-token";
    private static final String TEST_EMAIL = "user@test.com";
    private static final String ADMIN_EMAIL = "admin@kin.com";
    private static final String TEST_ROLE = "FREE";
    private static final String ADMIN_ROLE = "ADMIN";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_deberiaAutenticarUsuario_conTokenValido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(VALID_TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtService.extractRole(VALID_TOKEN)).thenReturn(TEST_ROLE);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(TEST_EMAIL, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FREE")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_deberiaContinuarSinAutenticar_cuandoNoHayHeaderAuthorization() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(any());
    }

    @Test
    void doFilter_deberiaContinuarSinAutenticar_cuandoHeaderNoEmpiezaConBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic some-token");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(any());
    }

    @Test
    void doFilter_noDeberiaAutenticar_conTokenInvalidoOExpirado() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
        when(jwtService.isTokenValid(INVALID_TOKEN)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService).isTokenValid(INVALID_TOKEN);
        verify(jwtService, never()).extractEmail(any());
        verify(jwtService, never()).extractRole(any());
    }

    @Test
    void doFilter_deberiaExtraerRolCorrectamente_delTokenValido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(VALID_TOKEN)).thenReturn(ADMIN_EMAIL);
        when(jwtService.extractRole(VALID_TOKEN)).thenReturn(ADMIN_ROLE);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(ADMIN_EMAIL, authentication.getPrincipal());
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        verify(jwtService).extractEmail(VALID_TOKEN);
        verify(jwtService).extractRole(VALID_TOKEN);
    }
}
