package com.kinplatform.auth;

import com.kinplatform.auth.dto.AuthResponse;
import com.kinplatform.auth.dto.LoginRequest;
import com.kinplatform.auth.dto.RegisterRequest;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_FULLNAME = "Test User";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";
    private static final String JWT_TOKEN = "jwt-token-value";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail(TEST_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setFullName(TEST_FULLNAME);

        loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        mockUser = User.builder()
                .id(USER_ID)
                .email(TEST_EMAIL)
                .passwordHash(ENCODED_PASSWORD)
                .fullName(TEST_FULLNAME)
                .role(UserRole.FREE)
                .build();
    }

    @Test
    void register_deberiaCrearUsuarioExitosamente_cuandoEmailNoExiste() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(USER_ID, TEST_EMAIL, UserRole.FREE.name())).thenReturn(JWT_TOKEN);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(JWT_TOKEN, response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_FULLNAME, response.getFullName());
        assertEquals(UserRole.FREE.name(), response.getRole());

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userRepository).save(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertEquals(TEST_EMAIL, captured.getEmail());
        assertEquals(ENCODED_PASSWORD, captured.getPasswordHash());
        assertEquals(TEST_FULLNAME, captured.getFullName());
        assertEquals(UserRole.FREE, captured.getRole());
        assertEquals(10, captured.getCredits());
    }

    @Test
    void register_deberiaLanzarExcepcion_cuandoEmailYaExiste() {
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertTrue(ex.getMessage().contains("Email already registered"));
        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_deberiaRetornarTokenValido_conCredencialesCorrectas() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(USER_ID, TEST_EMAIL, UserRole.FREE.name())).thenReturn(JWT_TOKEN);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals(JWT_TOKEN, response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_FULLNAME, response.getFullName());
        assertEquals(UserRole.FREE.name(), response.getRole());

        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(jwtService).generateToken(USER_ID, TEST_EMAIL, UserRole.FREE.name());
    }

    @Test
    void login_deberiaLanzarExcepcion_conPasswordIncorrecta() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_deberiaLanzarExcepcion_cuandoUsuarioNoExiste() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}
