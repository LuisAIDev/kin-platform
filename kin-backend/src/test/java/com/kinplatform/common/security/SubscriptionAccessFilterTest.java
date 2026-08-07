package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessFilterTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private SubscriptionValidatorService validatorService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SubscriptionAccessFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SubscriptionAccessFilter(validatorService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of(() -> "ROLE_FREE")));
    }

    @Test
    void creacionDeProyecto_bloqueada_deberiaDevolver403() throws Exception {
        authenticate("a@kin.com");
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getMethod()).thenReturn("POST");
        when(userRepository.findByEmail("a@kin.com"))
                .thenReturn(Optional.of(
                        User.builder().id(USER_ID).email("a@kin.com").build()));
        when(validatorService.canCreateProject(USER_ID)).thenReturn(false);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void creacionDeProyecto_permitida_deberiaPasar() throws Exception {
        authenticate("a@kin.com");
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getMethod()).thenReturn("POST");
        when(userRepository.findByEmail("a@kin.com"))
                .thenReturn(Optional.of(
                        User.builder().id(USER_ID).email("a@kin.com").build()));
        when(validatorService.canCreateProject(USER_ID)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void envioDeMensaje_bloqueado_deberiaDevolver403() throws Exception {
        authenticate("a@kin.com");
        when(request.getServletPath()).thenReturn("/projects/p1/chat");
        when(request.getRequestURI()).thenReturn("/api/v1/projects/p1/chat");
        when(request.getMethod()).thenReturn("POST");
        when(userRepository.findByEmail("a@kin.com"))
                .thenReturn(Optional.of(
                        User.builder().id(USER_ID).email("a@kin.com").build()));
        when(validatorService.canSendMessage(USER_ID)).thenReturn(false);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void sinAutenticacion_deberiaPasar() throws Exception {
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usuarioInexistente_deberiaPasar() throws Exception {
        authenticate("ghost@kin.com");
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getMethod()).thenReturn("POST");
        when(userRepository.findByEmail("ghost@kin.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void getNoDeberiaFiltrarse() throws Exception {
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getMethod()).thenReturn("GET");

        boolean skip = filter.shouldNotFilter(request);

        assertEquals(true, skip);
    }
}
