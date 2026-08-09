package com.kinplatform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.auth.dto.AuthResponse;
import com.kinplatform.auth.dto.LoginRequest;
import com.kinplatform.auth.dto.RegisterRequest;
import com.kinplatform.auth.dto.UserDTO;
import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.auth.verification.EmailVerificationTokenService;
import com.kinplatform.auth.verification.VerifyEmailOutcome;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "user@kin.com";
    private static final String TOKEN = "jwt-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailVerificationTokenService tokenService;

    @Mock
    private EmailSender emailSender;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, tokenService, emailSender);
    }

    private RegisterRequest registerRequest() {
        var req = new RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("KINpass123!a");
        req.setFullName("KIN User");
        return req;
    }

    private User unverifiedUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .emailVerified(false)
                .build();
    }

    private User verifiedUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .emailVerified(true)
                .build();
    }

    @Test
    void register_deberiaCrearUsuarioNoVerificadoYNoEntregarToken() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("KINpass123!a")).thenReturn("hashed");
        var user = unverifiedUser();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        AuthResponse response = authService.register(registerRequest());

        assertNull(response.getToken());
        assertFalse(response.getEmailVerified());
        assertEquals(EMAIL, response.getEmail());
        assertEquals("FREE", response.getRole());
        verify(tokenService).createForUser(user);
        verify(emailSender).sendVerificationEmail(EMAIL, "KIN User", "http://localhost:3000/verify-email?token=verify-token");
    }

    @Test
    void register_conEmailExistente_deberiaResponderGenericoSinEnumeracion() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        AuthResponse response = authService.register(registerRequest());

        assertNull(response.getToken());
        assertFalse(response.getEmailVerified());
        assertEquals(EMAIL, response.getEmail());
        verify(tokenService, never()).createForUser(any(User.class));
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void register_passwordCorta_deberiaFallar() {
        var req = registerRequest();
        req.setPassword("corta");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_passwordSinVariacion_deberiaFallar() {
        var req = registerRequest();
        req.setPassword("abcdefghijklmn");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void login_conCredencialesValidasYVerificado_deberiaDevolverToken() {
        var user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), EMAIL, "FREE")).thenReturn(TOKEN);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        AuthResponse response = authService.login(req);

        assertEquals(TOKEN, response.getToken());
        assertTrue(response.getEmailVerified());
    }

    @Test
    void login_noVerificado_deberiaLanzarEmailVerificationRequired() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        var ex = assertThrows(EmailVerificationRequiredException.class, () -> authService.login(req));

        assertTrue(ex.getMessage().contains("no ha sido verificado"));
    }

    @Test
    void login_conPasswordIncorrecto_deberiaLanzar() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("wrong");

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void login_conEmailInexistente_deberiaLanzar() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void getCurrentUser_conTokenValido_deberiaDevolverDatosConEmailVerified() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.PREMIUM)
                .credits(10)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser(TOKEN);

        assertEquals(EMAIL, dto.getEmail());
        assertEquals("PREMIUM", dto.getRole());
        assertTrue(dto.getEmailVerified());
    }

    @Test
    void getCurrentUser_conTokenInvalido_deberiaLanzar() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.getCurrentUser(TOKEN));
    }

    @Test
    void getCurrentUser_conUsuarioInexistente_deberiaLanzar() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.getCurrentUser(TOKEN));
    }

    @Test
    void logout_deberiaBlacklistearElToken() {
        authService.logout(TOKEN);

        verify(jwtService).blacklistToken(TOKEN);
    }

    @Test
    void logout_conTokenNulo_noDeberiaBlacklistear() {
        authService.logout(null);

        verify(jwtService, never()).blacklistToken(anyString());
    }

    @Test
    void register_deberiaMinusculizarElEmail() {
        when(userRepository.existsByEmail("user@kin.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("user@kin.com")
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .emailVerified(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        var req = registerRequest();
        req.setEmail("USER@KIN.COM");

        authService.register(req);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyEmail_deberiaDelegarEnElTokenService() {
        when(tokenService.verify("tok")).thenReturn(VerifyEmailOutcome.SUCCESS);

        assertEquals(VerifyEmailOutcome.SUCCESS, authService.verifyEmail("tok"));
    }

    @Test
    void verifyEmail_noDeberiaGenerarJwtNiCrearSesion() {
        when(tokenService.verify("tok")).thenReturn(VerifyEmailOutcome.SUCCESS);

        authService.verifyEmail("tok");

        verify(jwtService, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void resend_conUsuarioNoVerificadoYFueraDeCooldown_deberiaEnviar() {
        var user = unverifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.isWithinCooldown(user.getId())).thenReturn(false);
        when(tokenService.createForUser(user)).thenReturn("new-token");

        authService.resendVerification(EMAIL);

        verify(emailSender).sendVerificationEmail(EMAIL, "KIN User", "http://localhost:3000/verify-email?token=new-token");
    }

    @Test
    void resend_conEmailInexistente_noDeberiaEnviarNada() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        authService.resendVerification(EMAIL);

        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resend_conUsuarioVerificado_noDeberiaEnviarNada() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));

        authService.resendVerification(EMAIL);

        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resend_dentroDeCooldown_noDeberiaEnviarNada() {
        var user = unverifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.isWithinCooldown(user.getId())).thenReturn(true);

        authService.resendVerification(EMAIL);

        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }
}
