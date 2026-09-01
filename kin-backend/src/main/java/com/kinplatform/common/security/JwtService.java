package com.kinplatform.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            Environment environment) {
        validateSecret(secret, environment);
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    private void validateSecret(String secret, Environment environment) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no está configurado. Define la variable de entorno JWT_SECRET "
                    + "con un secreto Base64 de al menos 256 bits (32 bytes) para HS256.");
        }
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            throw new IllegalStateException("JWT_SECRET no es Base64 válido: " + e.getMessage());
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET demasiado corto: " + decoded.length + " bytes. "
                    + "HS256 requiere al menos " + MIN_SECRET_BYTES + " bytes (256 bits).");
        }
        boolean isProd = environment.acceptsProfiles("prod", "production");
        if (isProd
                && ("a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm"
                                .equals(secret)
                        || "kin-platform-secure-jwt-secret-for-production-certification-2026-0123456789abcdef"
                                .equals(secret))) {
            throw new IllegalStateException("JWT_SECRET en producción no puede usar el valor de prueba por defecto. "
                    + "Genera un secreto único con: openssl rand -base64 32");
        }
    }

    public String generateToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, "access", expirationMs);
    }

    /**
     * Genera un refresh token (tipo {@code refresh}) con mayor expiración, para
     * renovar el access token sin re-autenticar (fase de producción).
     */
    public String generateRefreshToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, "refresh", refreshExpirationMs);
    }

    private String buildToken(UUID userId, String email, String role, String type, long ttlMs) {
        var now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Valida un refresh token y, si es válido (tipo {@code refresh}), emite un
     * nuevo access token. Devuelve {@code null} si el token no es un refresh
     * válido (seguridad: un access token nunca se usa para renovar).
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            if (isBlacklisted(refreshToken)) {
                return null;
            }
            var claims = parseClaims(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                return null;
            }
            return buildToken(
                    UUID.fromString(claims.get("userId", String.class)),
                    claims.getSubject(),
                    claims.get("role", String.class),
                    "access",
                    expirationMs);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            if (isBlacklisted(token)) {
                return false;
            }
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void blacklistToken(String token) {
        try {
            long exp = parseClaims(token).getExpiration().getTime();
            blacklistedTokens.put(token, exp);
        } catch (Exception e) {
            blacklistedTokens.put(token, System.currentTimeMillis() + 60_000L);
        }
    }

    private boolean isBlacklisted(String token) {
        Long exp = blacklistedTokens.get(token);
        if (exp == null) {
            return false;
        }
        if (exp < System.currentTimeMillis()) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
