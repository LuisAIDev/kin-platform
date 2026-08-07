package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString("kin-test-secret-key-for-unit-tests-0123456789".getBytes());
        jwtService = new JwtService(secret, 86_400_000L);
    }

    @Test
    void generateToken_deberiaCrearTokenValido() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "a@kin.com", "ADMIN");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("a@kin.com", jwtService.extractEmail(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void tokenManipulado_deberiaSerInvalido() {
        String token = jwtService.generateToken(UUID.randomUUID(), "a@kin.com", "FREE");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void tokenBasura_deberiaSerInvalido() {
        assertFalse(jwtService.isTokenValid("not.a.jwt"));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void tokenDeOtraClave_deberiaSerInvalido() {
        String otherSecret =
                Base64.getEncoder().encodeToString("kin-other-secret-key-for-different-signature".getBytes());
        JwtService other = new JwtService(otherSecret, 86_400_000L);
        String token = other.generateToken(UUID.randomUUID(), "b@kin.com", "FREE");

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void tokenBlacklistado_deberiaSerInvalido() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "a@kin.com", "FREE");
        assertTrue(jwtService.isTokenValid(token));

        jwtService.blacklistToken(token);

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void blacklistToken_deberiaMantenerValidoOtroToken() {
        String t1 = jwtService.generateToken(UUID.randomUUID(), "a@kin.com", "FREE");
        String t2 = jwtService.generateToken(UUID.randomUUID(), "b@kin.com", "FREE");

        jwtService.blacklistToken(t1);

        assertFalse(jwtService.isTokenValid(t1));
        assertTrue(jwtService.isTokenValid(t2));
    }
}
