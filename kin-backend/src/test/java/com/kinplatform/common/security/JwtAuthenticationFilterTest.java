package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.user.PhysicianVerificationStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
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
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User user(UserRole role, PhysicianVerificationStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("a@kin.com")
                .role(role)
                .physicianVerificationStatus(status)
                .build();
    }

    private void stubValidToken(String token, String email) {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(email);
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
        stubValidToken("valid", "a@kin.com");
        when(userRepository.findByEmail("a@kin.com")).thenReturn(Optional.of(user(UserRole.ADMIN, null)));

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
        when(userRepository.findByEmail("cookie@kin.com")).thenReturn(Optional.of(user(UserRole.FREE, null)));

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

    @Test
    void tokenValido_conUsuarioInexistente_deberiaPasarSinAutenticar() throws Exception {
        stubValidToken("valid", "ghost@kin.com");
        when(userRepository.findByEmail("ghost@kin.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ------------------------------------------------------------------
    // Capacidad profesional derivada del estado persistido (Alternativa B):
    // un usuario con physician_verification_status=APPROVED obtiene
    // ROLE_PHYSICIAN sin necesidad de relogin ni de cambiar users.role.
    // ------------------------------------------------------------------

    @Test
    void freeAprobado_deberiaObtenerROLE_PHYSICIAN_sinRelogin() throws Exception {
        stubValidToken("valid", "a@kin.com");
        var approved = user(UserRole.FREE, PhysicianVerificationStatus.APPROVED);
        when(userRepository.findByEmail("a@kin.com")).thenReturn(Optional.of(approved));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FREE")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PHYSICIAN")));
        verify(request).setAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE, approved);
    }

    @Test
    void freePendiente_noDeberiaObtenerROLE_PHYSICIAN() throws Exception {
        stubValidToken("valid", "a@kin.com");
        when(userRepository.findByEmail("a@kin.com"))
                .thenReturn(Optional.of(user(UserRole.FREE, PhysicianVerificationStatus.PENDING)));

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FREE")));
        assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PHYSICIAN")));
    }

    @Test
    void physicianPendiente_noDeberiaObtenerROLE_PHYSICIAN_legacy() throws Exception {
        stubValidToken("valid", "a@kin.com");
        when(userRepository.findByEmail("a@kin.com"))
                .thenReturn(Optional.of(user(UserRole.PHYSICIAN, PhysicianVerificationStatus.PENDING)));

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PHYSICIAN")));
    }

    @Test
    void physicianAprobado_deberiaObtenerROLE_PHYSICIAN() throws Exception {
        stubValidToken("valid", "a@kin.com");
        when(userRepository.findByEmail("a@kin.com"))
                .thenReturn(Optional.of(user(UserRole.PHYSICIAN, PhysicianVerificationStatus.APPROVED)));

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PHYSICIAN")));
    }
}
