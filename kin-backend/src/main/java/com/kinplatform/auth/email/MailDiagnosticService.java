package com.kinplatform.auth.email;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Diagnóstico de SMTP (solo ADMIN, {@code GET /admin/health/email/diagnostic}).
 *
 * <p>Comprueba la configuración actual (con credenciales enmascaradas), prueba
 * la conexión SMTP autenticando con las credenciales vigentes y, opcionalmente,
 * envía un correo de prueba a un destinatario (query param {@code to} o
 * {@code app.mail.diagnostic-to} / {@code MAIL_DIAGNOSTIC_TO}).</p>
 */
@Service
@RequiredArgsConstructor
public class MailDiagnosticService {

    private final Environment environment;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:KIN Platform}")
    private String fromName;

    @Value("${app.mail.diagnostic-to:}")
    private String diagnosticTo;

    public DiagnosticResult diagnose(String requestedTo) {
        String target = (requestedTo == null || requestedTo.isBlank()) ? diagnosticTo : requestedTo.trim();
        List<String> logs = new ArrayList<>();

        boolean connected = false;
        String connectionError = null;
        try {
            Session session = createSession();
            try (Transport transport = session.getTransport("smtp")) {
                transport.connect(host, port, username, password);
                connected = true;
                logs.add("Conexión SMTP OK a " + host + ":" + port);
            }
        } catch (Exception e) {
            connectionError = e.getMessage();
            logs.add("Fallo de conexión SMTP: " + e.getMessage());
        }

        boolean sent = false;
        String sendError = null;
        if (connected && target != null && !target.isBlank()) {
            try {
                sendTestEmail(target);
                sent = true;
                logs.add("Correo de prueba enviado a " + maskEmail(target));
            } catch (Exception e) {
                sendError = e.getMessage();
                logs.add("Fallo al enviar el correo de prueba: " + e.getMessage());
            }
        } else if (connected && (target == null || target.isBlank())) {
            logs.add("Conexión OK. No se envió correo de prueba: falta destinatario "
                    + "(query param 'to' o app.mail.diagnostic-to / MAIL_DIAGNOSTIC_TO)");
        }

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enabled", enabled);
        config.put("host", host);
        config.put("port", port);
        config.put("from", maskEmail(from));
        config.put("fromName", fromName);
        config.put("username", maskUsername(username));
        config.put("smtpAuth", smtpAuth);
        config.put("diagnosticTo", maskEmail(target));
        config.put("activeProfiles", environment == null ? List.of() : Arrays.asList(environment.getActiveProfiles()));

        return new DiagnosticResult(config, connected, connectionError, sent, sendError, List.copyOf(logs));
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        if (port == 465) {
            // Implicit TLS (SMTPS): sin STARTTLS.
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        }
        return Session.getInstance(props);
    }

    private void sendTestEmail(String to) throws Exception {
        Session session = createSession();
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from, fromName));
        message.setRecipients(Message.RecipientType.TO, to);
        message.setSubject("KIN — prueba de diagnóstico SMTP");
        message.setText("Correo de prueba del diagnóstico SMTP de KIN.\n\n"
                + "Si recibes este mensaje, el envío funciona correctamente.");
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(host, port, username, password);
            transport.sendMessage(message, message.getAllRecipients());
        }
    }

    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email == null ? "(sin configurar)" : "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? "**" : local.charAt(0) + "**" + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }

    private static String maskUsername(String value) {
        if (value == null || value.isBlank()) {
            return "(sin configurar)";
        }
        return value.length() <= 3 ? "***" : value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    public record DiagnosticResult(
            Map<String, Object> config,
            boolean connected,
            String connectionError,
            boolean testEmailSent,
            String sendError,
            List<String> logs) {}
}
