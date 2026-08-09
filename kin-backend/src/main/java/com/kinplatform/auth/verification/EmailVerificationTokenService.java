package com.kinplatform.auth.verification;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Generación y consumo de tokens de verificación de correo.
 *
 * <p>El token original es aleatorio criptográficamente seguro (32 bytes,
 * Base64URL sin padding). Solo se persiste su hash SHA-256. Es de un solo
 * uso, con expiración, y los tokens previos del mismo usuario se invalidan
 * al generar uno nuevo.</p>
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${app.verify.token-ttl-minutes:1440}")
    private long ttlMinutes;

    @Value("${app.mail.resend-cooldown-seconds:60}")
    private long cooldownSeconds;

    /**
     * Crea un token para el usuario y devuelve el token original (para
     * incluirlo en el correo). Solo el hash queda persistido.
     */
    @Transactional
    public String createForUser(User user) {
        invalidatePrevious(user.getId());
        String token = randomToken();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(hash(token))
                .expiresAt(OffsetDateTime.now().plusMinutes(ttlMinutes))
                .build());
        return token;
    }

    /** Verifica si un reenvío es permitido (cooldown por usuario). */
    public boolean isWithinCooldown(UUID userId) {
        return tokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(t -> t.getCreatedAt().plusSeconds(cooldownSeconds).isAfter(OffsetDateTime.now()))
                .orElse(false);
    }

    /** Verifica un token: válido, expirado, ya usado o inexistente/manipulado. */
    @Transactional
    public VerifyEmailOutcome verify(String token) {
        if (token == null || token.isBlank()) {
            return VerifyEmailOutcome.INVALID;
        }
        var entity = tokenRepository.findByTokenHash(hash(token)).orElse(null);
        if (entity == null) {
            return VerifyEmailOutcome.INVALID;
        }
        if (entity.getUsedAt() != null) {
            return VerifyEmailOutcome.ALREADY_USED;
        }
        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return VerifyEmailOutcome.EXPIRED;
        }

        var user = userRepository.findById(entity.getUserId()).orElse(null);
        if (user == null) {
            return VerifyEmailOutcome.INVALID;
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        entity.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(entity);
        invalidatePrevious(user.getId());

        return VerifyEmailOutcome.SUCCESS;
    }

    private void invalidatePrevious(UUID userId) {
        tokenRepository.markAllUsedForUser(userId, OffsetDateTime.now());
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
