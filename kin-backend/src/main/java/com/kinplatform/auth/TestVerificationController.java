package com.kinplatform.auth;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test hook EXCLUSIVO del perfil {@code test} ({@code @Profile("test")}).
 *
 * <p>Recupera el enlace de verificación que {@code LoggingEmailSender} capturó
 * en memoria, para que los E2E de Playwright puedan ejercitar el endpoint REAL
 * {@code GET /auth/verify-email?token=...}. NO marca usuarios como verificados
 * y NO está disponible en producción: si el perfil activo no es {@code test},
 * este bean no existe en el contexto de Spring.</p>
 */
@RestController
@RequestMapping("/auth/test")
@Profile("test")
@RequiredArgsConstructor
public class TestVerificationController {

    private final TestVerificationStore store;

    @GetMapping("/verification-link")
    public ResponseEntity<Map<String, String>> verificationLink(@RequestParam("email") String email) {
        String link = store.get(email);
        if (link == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("link", link));
    }
}
