package com.kinplatform.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Almacén en memoria (SOLO perfil {@code test}) que captura temporalmente
 * {@code email → enlace de verificación} generado por el
 * {@code LoggingEmailSender}. Permite que Playwright recupere el enlace real
 * (y el token) y ejercite el endpoint real {@code /auth/verify-email} sin
 * depender de SMTP/Brevo/TLS.
 *
 * <p>Thread-safe (ConcurrentHashMap). No persiste en PostgreSQL; vive solo
 * mientras el proceso está activo. Nunca se expone en perfiles de
 * producción: el bean solo existe bajo {@code @Profile("test")}.</p>
 */
@Component
@Profile("test")
public class TestVerificationStore {

    private final Map<String, String> linksByEmail = new ConcurrentHashMap<>();

    public void put(String email, String verificationLink) {
        linksByEmail.put(email, verificationLink);
    }

    public String get(String email) {
        return linksByEmail.get(email);
    }

    public String remove(String email) {
        return linksByEmail.remove(email);
    }

    public void clear() {
        linksByEmail.clear();
    }

    public boolean isEmpty() {
        return linksByEmail.isEmpty();
    }

    public int size() {
        return linksByEmail.size();
    }
}
