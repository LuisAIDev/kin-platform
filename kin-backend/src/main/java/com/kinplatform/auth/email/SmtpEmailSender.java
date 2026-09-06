package com.kinplatform.auth.email;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envío real de correo vía SMTP (activado con {@code app.mail.enabled=true}).
 * Si SMTP no está configurado correctamente, falla de forma explícita y
 * segura (arranque o envío) — nunca cae silenciosamente en el
 * {@link LoggingEmailSender}.
 *
 * <p>NO usa {@code @ConditionalOnBean(JavaMailSender.class)}: el
 * {@code JavaMailSender} lo registra la auto-configuración de Spring Boot,
 * que se procesa después del escaneo de componentes, por lo que la condición
 * podría evaluarse como falsa y dejar a KIN sin ningún {@code EmailSender}.
 * En su lugar se inyecta un {@link ObjectProvider} y se resuelve (fail-fast)
 * en {@code @PostConstruct}.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private JavaMailSender mailSender;

    private final Counter attemptsCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer latencyTimer;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider, MeterRegistry meterRegistry) {
        this.mailSenderProvider = mailSenderProvider;
        this.attemptsCounter = Counter.builder("kin_email_attempts_total")
                .tag("sender", "smtp")
                .register(meterRegistry);
        this.successCounter =
                Counter.builder("kin_email_success_total").tag("sender", "smtp").register(meterRegistry);
        this.failureCounter =
                Counter.builder("kin_email_failure_total").tag("sender", "smtp").register(meterRegistry);
        this.latencyTimer =
                Timer.builder("kin_email_latency_seconds").tag("sender", "smtp").register(meterRegistry);
    }

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:KIN Platform}")
    private String fromName;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private String mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    /**
     * Feature flag de último recurso: si el envío falla y el destinatario está en
     * {@link #debugFallbackAllowlist}, el enlace de verificación se imprime en los
     * logs (WARN). NUNCA se activa sin whitelist explícita. Default {@code false}.
     */
    @Value("${app.mail.debug-fallback:false}")
    private boolean debugFallback;

    /** Whitelist de destinatarios para el fallback (emails o dominios {@code @dominio}). */
    @Value("${app.mail.debug-fallback-allowlist:}")
    private String debugFallbackAllowlist;

    /** Perfiles activos (comma-separated) para el log DEBUG del contenido (solo dev/test). */
    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @PostConstruct
    public void validate() {
        mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("app.mail.enabled=true pero no hay un JavaMailSender disponible "
                    + "(revisa spring.mail.host / MAIL_HOST). Si SMTP no está configurado, "
                    + "la aplicación no puede enviar correos de verificación.");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("app.mail.enabled=true pero app.mail.from (MAIL_FROM) no está configurado");
        }
        if (mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero spring.mail.host (MAIL_HOST) no está configurado");
        }
        if (smtpAuth
                && (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank())) {
            throw new IllegalStateException(
                    "app.mail.enabled=true con mail.smtp.auth=true pero MAIL_USERNAME/MAIL_PASSWORD no están configurados");
        }
        log.info(
                "SMTP habilitado: host={}:{} from={} smtpAuth={} — los correos se envían por SMTP real",
                mailHost,
                mailPort,
                maskEmail(from),
                smtpAuth);
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String verificationLink) {
        sendEmail(
                "verification",
                to,
                fullName,
                verificationLink,
                "Verifica tu correo electrónico en KIN",
                "Hola " + fullName + ",\n\n"
                        + "Para activar tu cuenta de KIN, abre este enlace:\n\n"
                        + verificationLink + "\n\n"
                        + "El enlace es de un solo uso y expira en 24 horas.\n\n"
                        + "Si no creaste esta cuenta, ignora este mensaje.");
    }

    @Override
    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        sendEmail(
                "password-reset",
                to,
                fullName,
                resetLink,
                "Recupera tu contraseña de KIN",
                "Hola " + fullName + ",\n\n"
                        + "Para restablecer tu contraseña de KIN, abre este enlace:\n\n"
                        + resetLink + "\n\n"
                        + "El enlace es de un solo uso y expira en 24 horas.\n\n"
                        + "Si no solicitaste este cambio, ignora este mensaje.");
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
        String specialtySuffix = (specialty == null || specialty.isBlank()) ? "" : " (" + specialty + ")";
        String optionalMessage = (message == null || message.isBlank()) ? "" : "\n" + message + "\n";
        String consentHint = consentRequired
                ? "\nIMPORTANTE: para unirte a " + physicianName
                        + " debes aceptar primero el consentimiento de tratamiento de datos de salud desde tu panel.\n"
                : "";
        sendEmail(
                "invitation",
                to,
                patientName,
                invitationLink,
                "Has recibido una invitación de tu médico en KIN Salud",
                "Hola " + patientName + ",\n\n"
                        + "El médico " + physicianName + specialtySuffix
                        + " te ha invitado a conectarte a través de KIN Salud."
                        + optionalMessage + "\n"
                        + consentHint + "\n"
                        + "Para aceptar o rechazar esta invitación, abre tu panel de invitaciones:\n\n"
                        + invitationLink + "\n\n"
                        + "Si no reconoces a este médico, puedes ignorar este mensaje.");
    }

    @Override
    public void sendAppointmentReminderEmail(
            String to, String patientName, String physicianName, String scheduledAtText) {
        sendEmail(
                "appointment-reminder",
                to,
                patientName,
                "https://kin-platform.com/dashboard/patient/schedule",
                "Recordatorio de tu cita en KIN Salud",
                "Hola " + patientName + ",\n\n"
                        + "Tienes una cita confirmada con " + physicianName + " el " + scheduledAtText + ".\n\n"
                        + "Puedes ver y gestionar tu cita en tu panel de KIN Salud:\n"
                        + "https://kin-platform.com/dashboard/patient/schedule\n\n"
                        + "Si no puedes asistir, cancela o reprograma con anticipación.");
    }

    private void sendEmail(String type, String to, String fullName, String link, String subject, String text) {
        attemptsCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
            String messageId = getMessageId(message);
            log.info("Correo de {} ACEPTADO POR SMTP para {} messageId={}", type, maskEmail(to), messageId);
            successCounter.increment();
        } catch (MessagingException | UnsupportedEncodingException e) {
            failureCounter.increment();
            log.error(
                    "Fallo SMTP al enviar correo de {} para {} — asunto '{}': {}",
                    type,
                    maskEmail(to),
                    subject,
                    e.getMessage(),
                    e);
            logDebugBody(to, subject, text);
            if (debugFallback && isFallbackAllowed(to)) {
                log.warn(
                        "[MAIL_DEBUG_FALLBACK] No se pudo enviar el correo de {} a {}; "
                                + "enlace (último recurso): {}",
                        type,
                        maskEmail(to),
                        link);
            }
            throw new IllegalStateException("No se pudo enviar el correo de " + type, e);
        } finally {
            sample.stop(latencyTimer);
        }
    }

    /**
     * Log DEBUG con el contenido completo del correo, SOLO en perfiles
     * dev/test (nunca en producción) y con el nivel de log DEBUG activo.
     */
    private void logDebugBody(String to, String subject, String text) {
        boolean devOrTest =
                activeProfiles != null && (activeProfiles.contains("dev") || activeProfiles.contains("test"));
        if (devOrTest && log.isDebugEnabled()) {
            log.debug("[SMTP][debug] Contenido del correo para {} — asunto '{}':\n{}", maskEmail(to), subject, text);
        }
    }

    /**
     * Determina si un destinatario está en la whitelist del fallback de
     * último recurso (email exacto o dominio {@code @dominio}).
     */
    boolean isFallbackAllowed(String to) {
        if (debugFallbackAllowlist == null || debugFallbackAllowlist.isBlank() || to == null) {
            return false;
        }
        String recipient = to.toLowerCase().trim();
        return Arrays.stream(debugFallbackAllowlist.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .anyMatch(entry -> {
                    String e = entry.toLowerCase();
                    return recipient.equals(e) || (e.startsWith("@") && recipient.endsWith(e));
                });
    }

    private static String getMessageId(Message message) {
        try {
            String[] ids = message.getHeader("Message-ID");
            if (ids != null && ids.length > 0 && ids[0] != null) {
                return ids[0];
            }
        } catch (MessagingException ignored) {
        }
        return "unknown";
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
