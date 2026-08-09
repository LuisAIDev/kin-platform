package com.kinplatform.auth.email;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Envío de correo para desarrollo/tests SIN SMTP: registra el enlace de
 * verificación en el log.
 *
 * <p>Está <b>explícitamente prohibido en perfiles de producción</b>
 * ({@code prod}, {@code render}, {@code enterprise}): si se intenta activar
 * (p. ej. {@code APP_MAIL_ENABLED=false}) en esos perfiles, la aplicación
 * falla al arrancar en lugar de registrar tokens de verificación.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "render", "enterprise");

    private final Environment environment;

    @PostConstruct
    public void validateNotInProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILES.contains(profile)) {
                throw new IllegalStateException(
                        "LoggingEmailSender está prohibido en perfiles de producción ("
                                + String.join(", ", PRODUCTION_PROFILES)
                                + "). Configura SMTP (MAIL_HOST, MAIL_FROM, etc.) y APP_MAIL_ENABLED=true.");
            }
        }
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String verificationLink) {
        log.warn("[email-verification][dev/no-smtp] destinatario={} enlace={}", to, verificationLink);
    }
}
