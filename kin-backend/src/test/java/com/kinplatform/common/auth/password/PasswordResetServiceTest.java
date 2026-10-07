package com.kinplatform.common.auth.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.auth.email.EmailSender;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "user@kin.com";
    private static final String RAW_TOKEN = "token-mock-abc123";

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailSender emailSender;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokenRepository, userRepository, passwordEncoder, emailSender);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Test User")
                .isActive(true)
                .build();
    }

    @Test
    void requestReset_cuandoExisteUsuario_enviaCorreo() {
        var user = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.requestReset(EMAIL);

        verify(emailSender)
                .sendPasswordResetEmail(
                        org.mockito.ArgumentMatchers.eq(EMAIL),
                        org.mockito.ArgumentMatchers.eq("Test User"),
                        org.mockito.ArgumentMatchers.contains("/reset-password?token="));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void requestReset_cuandoNoExisteUsuario_noEnviaCorreo() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.requestReset(EMAIL);

        verify(emailSender, never()).sendPasswordResetEmail(any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void requestReset_cuandoUsuarioInactivo_noEnviaCorreo() {
        var user = user();
        user.setIsActive(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.requestReset(EMAIL);

        verify(emailSender, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void resetPassword_conTokenValido_actualizaContrasenaYUsaToken() {
        var user = user();
        var entity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash("hash")
                .expiresAt(java.time.OffsetDateTime.now().plusHours(1))
                .build();
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NuevaPass1!")).thenReturn("encoded");

        boolean ok = service.resetPassword(RAW_TOKEN, "NuevaPass1!");

        assertTrue(ok);
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(u -> "encoded".equals(u.getPasswordHash())));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_conTokenExpirado_noActualiza() {
        var user = user();
        var entity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash("hash")
                .expiresAt(java.time.OffsetDateTime.now().minusHours(1))
                .build();
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));

        boolean ok = service.resetPassword(RAW_TOKEN, "NuevaPass1!");

        assertFalse(ok);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_conTokenYaUsado_noActualiza() {
        var user = user();
        var entity = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash("hash")
                .expiresAt(java.time.OffsetDateTime.now().plusHours(1))
                .usedAt(java.time.OffsetDateTime.now())
                .build();
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(entity));

        boolean ok = service.resetPassword(RAW_TOKEN, "NuevaPass1!");

        assertFalse(ok);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_conContrasenaCorta_noActualiza() {
        var ok = service.resetPassword(RAW_TOKEN, "corta");

        assertFalse(ok);
        verify(tokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void generateResetLink_deberiaCrearTokenYDevolverUrl() {
        var user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        String link = service.generateResetLink(user.getId());

        assertTrue(link.startsWith("http://localhost:3000/reset-password?token="));
        assertTrue(link.length() > "http://localhost:3000/reset-password?token=".length());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void generateResetLink_usuarioInexistente_deberiaLanzar() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.generateResetLink(UUID.randomUUID()));
    }
}


