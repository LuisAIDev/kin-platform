package com.kinplatform.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.auth.dto.AuthResponse;
import com.kinplatform.auth.dto.LoginRequest;
import com.kinplatform.auth.dto.RegisterRequest;
import com.kinplatform.auth.dto.UserDTO;
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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    private RegisterRequest registerRequest() {
        var req = new RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("KINpass123!a");
        req.setFullName("KIN User");
        return req;
    }

    @Test
    void register_deberiaCrearUsuarioYDevolverToken() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("KINpass123!a")).thenReturn("hashed");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user.getId(), EMAIL, "FREE")).thenReturn(TOKEN);

        AuthResponse response = authService.register(registerRequest());

        assertEquals(TOKEN, response.getToken());
        assertEquals(EMAIL, response.getEmail());
        assertEquals("FREE", response.getRole());
    }

    @Test
    void register_conEmailExistente_deberiaLanzarMensajeGenerico() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest()));

        assertEquals("El email ya está registrado", ex.getMessage());
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
    void login_conCredencialesValidas_deberiaDevolverToken() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), EMAIL, "FREE")).thenReturn(TOKEN);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        AuthResponse response = authService.login(req);

        assertEquals(TOKEN, response.getToken());
    }

    @Test
    void login_conPasswordIncorrecto_deberiaLanzar() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
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
    void getCurrentUser_conTokenValido_deberiaDevolverDatos() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.PREMIUM)
                .credits(10)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser(TOKEN);

        assertEquals(EMAIL, dto.getEmail());
        assertEquals("PREMIUM", dto.getRole());
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

        verify(jwtService, org.mockito.Mockito.never()).blacklistToken(anyString());
    }

    @Test
    void register_deberiaMinusculizarElEmail() {
        when(userRepository.existsByEmail("USER@KIN.COM".toLowerCase())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("user@kin.com")
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user.getId(), "user@kin.com", "FREE")).thenReturn(TOKEN);

        var req = registerRequest();
        req.setEmail("USER@KIN.COM");

        authService.register(req);

        verify(userRepository).save(any(User.class));
    }
}
