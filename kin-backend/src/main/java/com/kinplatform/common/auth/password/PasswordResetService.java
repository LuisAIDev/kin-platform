package com.kinplatform.common.auth.password;

import com.kinplatform.common.auth.email.EmailSender;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitud y restablecimiento de contraseña.
 *
 * <p>El token original es aleatorio criptográficamente seguro (32 bytes,
 * Base64URL sin padding); solo se persiste su hash SHA-256. Es de un solo uso,
 * con expiración, y los tokens previos del usuario se invalidan al generar uno
 * nuevo. La solicitud nunca revela si el correo existe (anti-enumeración).</p>
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    @Value("${app.reset.token-ttl-minutes:1440}")
    private long ttlMinutes;

    @Value("${medical.frontend.base-url:https://www.kin-platform-medical.com}")
    private String frontendBaseUrl;

    /**
     * Solicita un enlace de recuperación. Siempre responde igual
     * independientemente de si el correo existe (anti-enumeración).
     */
    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        var user = userRepository.findByEmail(email.toLowerCase().trim()).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getIsActive())) {
            return;
        }
        String token = createForUser(user);
        String link = (frontendBaseUrl == null || frontendBaseUrl.isBlank() ? "http://localhost:3000" : frontendBaseUrl)
                + "/reset-password?token=" + token;
        emailSender.sendPasswordResetEmail(user.getEmail(), user.getFullName(), link);
        log.info("Enlace de recuperación de contraseña enviado a {}", maskEmail(user.getEmail()));
    }

    /**
     * Genera un enlace de restablecimiento para un usuario (sin enviar correo).
     * Uso operativo (solo ADMIN): permite a un administrador entregar el enlace
     * directamente cuando los correos no llegan. Invalida tokens anteriores.
     */
    @Transactional
    public String generateResetLink(UUID userId) {
        var user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String token = createForUser(user);
        String base = frontendBaseUrl == null || frontendBaseUrl.isBlank() ? "http://localhost:3000" : frontendBaseUrl;
        String link = base + "/reset-password?token=" + token;
        log.info("Enlace de recuperación generado manualmente para {}", maskEmail(user.getEmail()));
        return link;
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email == null ? "invalid" : "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? "**" : local.charAt(0) + "**" + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }

    /**
     * Restablece la contraseña con un token válido. Devuelve {@code true} si
     * el cambio se aplicó; {@code false} si el token es inválido, expirado o
     * ya fue usado (el frontend muestra un mensaje genérico).
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.length() < 8) {
            return false;
        }
        var entity = tokenRepository.findByTokenHash(hash(token)).orElse(null);
        if (entity == null
                || entity.getUsedAt() != null
                || entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return false;
        }
        var user = userRepository.findById(entity.getUserId()).orElse(null);
        if (user == null) {
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        entity.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(entity);
        tokenRepository.markUsedForUser(entity.getUserId(), OffsetDateTime.now());
        return true;
    }

    private String createForUser(User user) {
        tokenRepository.markUsedForUser(user.getId(), OffsetDateTime.now());
        String token = randomToken();
        tokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(hash(token))
                .expiresAt(OffsetDateTime.now().plusMinutes(ttlMinutes))
                .build());
        return token;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}


