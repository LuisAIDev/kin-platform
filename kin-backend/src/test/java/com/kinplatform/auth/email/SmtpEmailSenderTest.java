package com.kinplatform.auth.email;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class SmtpEmailSenderTest {

    private SmtpEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpEmailSender(org.mockito.Mockito.mock(JavaMailSender.class));
        ReflectionTestUtils.setField(sender, "from", "no-reply@kin.test");
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
}
