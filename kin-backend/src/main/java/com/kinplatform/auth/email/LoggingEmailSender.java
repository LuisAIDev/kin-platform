package com.kinplatform.auth.email;

import com.kinplatform.auth.TestVerificationStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
 *
 * <p>Adicionalmente, cuando el {@link TestVerificationStore} existe (perfil
 * {@code test}), captura temporalmente {@code email → enlace} en memoria para
 * que los E2E de Playwright puedan ejercitar el endpoint real de verificación.
 * La captura es opcional (ObjectProvider) y nunca persiste nada.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "render", "enterprise");

    private final Environment environment;
    private final ObjectProvider<TestVerificationStore> storeProvider;
    private final MeterRegistry meterRegistry;

    private final Counter attemptsCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer latencyTimer;

    public LoggingEmailSender(
            Environment environment, ObjectProvider<TestVerificationStore> storeProvider, MeterRegistry meterRegistry) {
        this.environment = environment;
        this.storeProvider = storeProvider;
        this.meterRegistry = meterRegistry;
        this.attemptsCounter = Counter.builder("kin_email_attempts_total")
                .tag("sender", "logging")
                .register(meterRegistry);
        this.successCounter = Counter.builder("kin_email_success_total")
                .tag("sender", "logging")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("kin_email_failure_total")
                .tag("sender", "logging")
                .register(meterRegistry);
        this.latencyTimer = Timer.builder("kin_email_latency_seconds")
                .tag("sender", "logging")
                .register(meterRegistry);
    }

    @PostConstruct
    public void validateNotInProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILES.contains(profile)) {
                throw new IllegalStateException("LoggingEmailSender está prohibido en perfiles de producción ("
                        + String.join(", ", PRODUCTION_PROFILES)
                        + "). Configura SMTP (MAIL_HOST, MAIL_FROM, etc.) y APP_MAIL_ENABLED=true.");
            }
        }
        log.warn("CORREO DESHABILITADO (modo sin SMTP): el envío de verificación/recuperación SOLO se imprime "
                + "en los logs (buscar '[email-verification]' o '[password-reset]'). En un despliegue, si el "
                + "correo no llega, revisa en el entorno: SPRING_PROFILES_ACTIVE (debe incluir 'prod'/'render') "
                + "y APP_MAIL_ENABLED=true con MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM.");
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String verificationLink) {
        attemptsCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            log.warn("[email-verification][dev/no-smtp] destinatario={} enlace={}", maskEmail(to), verificationLink);
            storeProvider.ifAvailable(store -> store.put(to, verificationLink));
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            throw new IllegalStateException("No se pudo procesar el correo de verificación (logging)", e);
        } finally {
            sample.stop(latencyTimer);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        attemptsCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            log.warn("[password-reset][dev/no-smtp] destinatario={} enlace={}", maskEmail(to), resetLink);
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            throw new IllegalStateException("No se pudo procesar el correo de recuperación (logging)", e);
        } finally {
            sample.stop(latencyTimer);
        }
    }

    @Override
    public void sendInvitationEmail(
            String to,
            String patientName,
            String physicianName,
            String specialty,
            String message,
            String invitationLink,
            boolean consentRequired) {
        attemptsCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            log.warn(
                    "[email-invitation][dev/no-smtp] destinatario={} medico={} consentimientoPendiente={} enlace={}",
                    maskEmail(to),
                    physicianName,
                    consentRequired,
                    invitationLink);
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            throw new IllegalStateException("No se pudo procesar el correo de invitación (logging)", e);
        } finally {
            sample.stop(latencyTimer);
        }
    }

    @Override
    public void sendInvitationReminderEmail(
            String to,
            String patientName,
            String physicianName,
            String specialty,
            String message,
            String invitationLink,
            boolean consentRequired) {
        attemptsCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            log.warn(
                    "[email-invitation-reminder][dev/no-smtp] destinatario={} medico={} consentimientoPendiente={} enlace={}",
                    maskEmail(to),
                    physicianName,
                    consentRequired,
                    invitationLink);
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            throw new IllegalStateException("No se pudo procesar el correo de recordatorio de invitación (logging)", e);
        } finally {
            sample.stop(latencyTimer);
        }
    }

    @Override
    public void sendAppointmentReminderEmail(
            String to, String patientName, String physicianName, String scheduledAtText) {
        attemptsCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            log.warn(
                    "[appointment-reminder][dev/no-smtp] destinatario={} medico={} fecha={}",
                    maskEmail(to),
                    physicianName,
                    scheduledAtText);
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            throw new IllegalStateException("No se pudo procesar el correo de recordatorio (logging)", e);
        } finally {
            sample.stop(latencyTimer);
        }
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "invalid";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? "**" : local.charAt(0) + "**" + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}
