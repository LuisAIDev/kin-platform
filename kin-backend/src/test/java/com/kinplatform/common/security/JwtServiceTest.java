package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString("kin-test-secret-key-for-unit-tests-0123456789".getBytes());
        jwtService = new JwtService(secret, 86_400_000L, new MockEnvironment());
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
        JwtService other = new JwtService(otherSecret, 86_400_000L, new MockEnvironment());
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

    @Test
    void secretNulo_deberiaFallarEnConstructor() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(null, 86_400_000L, new MockEnvironment()));
        assertTrue(ex.getMessage().contains("JWT_SECRET no está configurado"));
    }

    @Test
    void secretVacio_deberiaFallarEnConstructor() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("", 86_400_000L, new MockEnvironment()));
        assertTrue(ex.getMessage().contains("JWT_SECRET no está configurado"));
    }

    @Test
    void secretNoBase64_deberiaFallarEnConstructor() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("not-base64!", 86_400_000L, new MockEnvironment()));
        assertTrue(ex.getMessage().contains("no es Base64 válido"));
    }

    @Test
    void secretMuyCorto_deberiaFallarEnConstructor() {
        // 16 bytes = 128 bits (insuficiente para HS256 que requiere 256 bits)
        String shortSecret = Base64.getEncoder().encodeToString("1234567890123456".getBytes());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(shortSecret, 86_400_000L, new MockEnvironment()));
        assertTrue(ex.getMessage().contains("demasiado corto"));
        assertTrue(ex.getMessage().contains("16 bytes"));
    }

    @Test
    void secretValido256Bits_deberiaFuncionar() {
        // 32 bytes = 256 bits (mínimo para HS256)
        String validSecret = Base64.getEncoder().encodeToString("kin-test-secret-key-32-bytes-long!!".getBytes());
        JwtService service = new JwtService(validSecret, 86_400_000L, new MockEnvironment());
        String token = service.generateToken(UUID.randomUUID(), "test@kin.com", "FREE");
        assertTrue(service.isTokenValid(token));
    }

    @Test
    void secretPorDefectoEnProd_deberiaFallar() {
        String defaultSecret = "a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm";
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService(defaultSecret, 86_400_000L, prodEnv));
        assertTrue(ex.getMessage().contains("no puede usar el valor de prueba por defecto"));
    }
}
