package com.kinplatform.auth.email;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envío real de correo vía SMTP (activado con {@code app.mail.enabled=true}).
 * Si SMTP no está configurado correctamente, falla de forma explícita y
 * segura (arranque o envío) — nunca cae silenciosamente en el
 * {@link LoggingEmailSender}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@ConditionalOnBean(JavaMailSender.class)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

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
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Verifica tu correo electrónico en KIN");
            helper.setText("Hola " + fullName + ",\n\n"
                    + "Para activar tu cuenta de KIN, abre este enlace:\n\n"
                    + verificationLink + "\n\n"
                    + "El enlace es de un solo uso y expira en 24 horas.\n\n"
                    + "Si no creaste esta cuenta, ignora este mensaje.", false);
            mailSender.send(message);
            log.info("Correo de verificación enviado a {}", to);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo enviar el correo de verificación", e);
        }
    }
}
