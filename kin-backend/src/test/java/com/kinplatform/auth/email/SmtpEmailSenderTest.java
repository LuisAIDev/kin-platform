package com.kinplatform.auth.email;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class SmtpEmailSenderTest {

    private SmtpEmailSender sender;
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        bf.registerSingleton("mailSender", mailSender);
        ObjectProvider<JavaMailSender> provider = bf.getBeanProvider(JavaMailSender.class);
        sender = new SmtpEmailSender(provider);
        ReflectionTestUtils.setField(sender, "from", "no-reply@kin.test");
        ReflectionTestUtils.setField(sender, "fromName", "KIN Platform");
        ReflectionTestUtils.setField(sender, "mailHost", "smtp.test.com");
        ReflectionTestUtils.setField(sender, "smtpAuth", true);
        ReflectionTestUtils.setField(sender, "mailUsername", "user");
        ReflectionTestUtils.setField(sender, "mailPassword", "pass");
    }

    @Test
    void configCompleta_noDeberiaFallar() {
        sender.validate();
    }

    @Test
    void conMailHostAusente_deberiaFallar() {
        ReflectionTestUtils.setField(sender, "mailHost", "");

        assertThrows(IllegalStateException.class, sender::validate);
    }

    @Test
    void sinJavaMailSenderDisponible_deberiaFallarConMensajeClaro() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        ObjectProvider<JavaMailSender> emptyProvider = bf.getBeanProvider(JavaMailSender.class);
        sender = new SmtpEmailSender(emptyProvider);
        ReflectionTestUtils.setField(sender, "from", "no-reply@kin.test");
        ReflectionTestUtils.setField(sender, "mailHost", "smtp.test.com");

        assertThrows(IllegalStateException.class, sender::validate);
    }

    @Test
    void conMailFromAusente_deberiaFallar() {
        ReflectionTestUtils.setField(sender, "from", "");

        assertThrows(IllegalStateException.class, sender::validate);
    }

    @Test
    void conAuthYUsernameAusente_deberiaFallar() {
        ReflectionTestUtils.setField(sender, "mailUsername", "");

        assertThrows(IllegalStateException.class, sender::validate);
    }

    @Test
    void conAuthYPasswordAusente_deberiaFallar() {
        ReflectionTestUtils.setField(sender, "mailPassword", "");

        assertThrows(IllegalStateException.class, sender::validate);
    }

    @Test
    void sinAuthConCredencialesVacias_noDeberiaFallar() {
        ReflectionTestUtils.setField(sender, "smtpAuth", false);
        ReflectionTestUtils.setField(sender, "mailUsername", "");
        ReflectionTestUtils.setField(sender, "mailPassword", "");

        sender.validate();
    }

    @Test
    void remitente_deberiaIncluirNombreYEmail() throws Exception {
        ReflectionTestUtils.setField(sender, "smtpAuth", false);
        ReflectionTestUtils.setField(sender, "from", "hola@kin-platform.com");
        ReflectionTestUtils.setField(sender, "fromName", "KIN Platform");

        final jakarta.mail.Message[] captured = new jakarta.mail.Message[1];
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(inv -> {
            captured[0] = inv.getArgument(0);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        sender.validate();
        sender.sendVerificationEmail("destino@example.com", "Ana", "https://kin-platform.com/verify-email?token=abc");

        String fromHeader = captured[0].getFrom()[0].toString();
        assertTrue(fromHeader.contains("KIN Platform"));
        assertTrue(fromHeader.contains("hola@kin-platform.com"));
    }
}
