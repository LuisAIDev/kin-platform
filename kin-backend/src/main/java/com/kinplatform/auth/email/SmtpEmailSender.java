package com.kinplatform.auth.email;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
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

    public SmtpEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:KIN Platform}")
    private String fromName;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    @PostConstruct
    public void validate() {
        mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero no hay un JavaMailSender disponible "
                            + "(revisa spring.mail.host / MAIL_HOST). Si SMTP no está configurado, "
                            + "la aplicación no puede enviar correos de verificación.");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero app.mail.from (MAIL_FROM) no está configurado");
        }
        if (mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero spring.mail.host (MAIL_HOST) no está configurado");
        }
        if (smtpAuth && (mailUsername == null || mailUsername.isBlank()
                || mailPassword == null || mailPassword.isBlank())) {
            throw new IllegalStateException(
                    "app.mail.enabled=true con mail.smtp.auth=true pero MAIL_USERNAME/MAIL_PASSWORD no están configurados");
        }
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String verificationLink) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject("Verifica tu correo electrónico en KIN");
            helper.setText("Hola " + fullName + ",\n\n"
                    + "Para activar tu cuenta de KIN, abre este enlace:\n\n"
                    + verificationLink + "\n\n"
                    + "El enlace es de un solo uso y expira en 24 horas.\n\n"
                    + "Si no creaste esta cuenta, ignora este mensaje.", false);
            mailSender.send(message);
            log.info("Correo de verificación enviado a {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo enviar el correo de verificación", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject("Recupera tu contraseña de KIN");
            helper.setText("Hola " + fullName + ",\n\n"
                    + "Para restablecer tu contraseña de KIN, abre este enlace:\n\n"
                    + resetLink + "\n\n"
                    + "El enlace es de un solo uso y expira en 24 horas.\n\n"
                    + "Si no solicitaste este cambio, ignora este mensaje.", false);
            mailSender.send(message);
            log.info("Correo de recuperación de contraseña enviado a {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo enviar el correo de recuperación de contraseña", e);
        }
    }
}
