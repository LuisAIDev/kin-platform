package com.kinplatform.common.auth.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    private EmailVerificationTokenService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationTokenService(tokenRepository, userRepository);
        ReflectionTestUtils.setField(service, "ttlMinutes", 1440L);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60L);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("a@kin.com")
                .passwordHash("h")
                .fullName("A")
                .emailVerified(false)
                .build();
    }

    private EmailVerificationToken token(UUID userId, OffsetDateTime expiresAt, OffsetDateTime usedAt) {
        return EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hash")
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();
    }

    @Test
    void createForUser_deberiaPersistirSoloElHash_yDevolverElTokenOriginal() {
        var user = user();

        String original = service.createForUser(user);

        var captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        var saved = captor.getValue();

        assertEquals(user.getId(), saved.getUserId());
        assertEquals(sha256Hex(original), saved.getTokenHash());
        assertNotEquals(original, saved.getTokenHash());
        assertTrue(saved.getExpiresAt().isAfter(OffsetDateTime.now()));
        assertNull(saved.getUsedAt());
        verify(tokenRepository).markAllUsedForUser(eq(user.getId()), any(OffsetDateTime.class));
    }

    @Test
    void verify_conTokenValido_deberiaMarcarVerificadoYUsado() {
        var user = user();
        var entity = token(user.getId(), OffsetDateTime.now().plusHours(1), null);
        String original = "some-token";
        when(tokenRepository.findByTokenHash(sha256Hex(original))).thenReturn(Optional.of(entity));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var outcome = service.verify(original);

        assertEquals(VerifyEmailOutcome.SUCCESS, outcome);
        assertTrue(user.getEmailVerified());
        verify(userRepository).save(user);
        verify(tokenRepository).save(entity);
        assertTrue(entity.getUsedAt() != null);
    }

    @Test
    void verify_conTokenInexistente_deberiaFallarComoInvalido() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertEquals(VerifyEmailOutcome.INVALID, service.verify("nope"));
    }

    @Test
    void verify_conTokenNulo_oVacio_deberiaFallarComoInvalido() {
        assertEquals(VerifyEmailOutcome.INVALID, service.verify(null));
        assertEquals(VerifyEmailOutcome.INVALID, service.verify("  "));
    }

    @Test
    void verify_conTokenExpirado_deberiaFallar() {
        var user = user();
        var entity = token(user.getId(), OffsetDateTime.now().minusSeconds(1), null);
        String original = "expired-token";
        when(tokenRepository.findByTokenHash(sha256Hex(original))).thenReturn(Optional.of(entity));

        assertEquals(VerifyEmailOutcome.EXPIRED, service.verify(original));
    }

    @Test
    void verify_conTokenYaUsado_deberiaFallar() {
        var user = user();
        var entity = token(user.getId(), OffsetDateTime.now().plusHours(1), OffsetDateTime.now());
        String original = "used-token";
        when(tokenRepository.findByTokenHash(sha256Hex(original))).thenReturn(Optional.of(entity));

        assertEquals(VerifyEmailOutcome.ALREADY_USED, service.verify(original));
    }

    @Test
    void isWithinCooldown_sinTokensPrevios_deberiaSerFalse() {
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        assertTrue(!service.isWithinCooldown(UUID.randomUUID()));
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}


