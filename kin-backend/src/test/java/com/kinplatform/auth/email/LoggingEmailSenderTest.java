package com.kinplatform.auth.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LoggingEmailSenderTest {

    @Test
    void enPerfilDev_noDeberiaFallar() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");

        var sender = new LoggingEmailSender(env);

        assertDoesNotThrow(sender::validateNotInProduction);
    }

    @Test
    void enPerfilProd_deberiaEstarProhibido() {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");

        var sender = new LoggingEmailSender(env);

        assertThrows(IllegalStateException.class, sender::validateNotInProduction);
    }

    @Test
    void enPerfilRender_deberiaEstarProhibido() {
        var env = new MockEnvironment();
        env.setActiveProfiles("render", "prod");

        var sender = new LoggingEmailSender(env);

        assertThrows(IllegalStateException.class, sender::validateNotInProduction);
    }

    @Test
    void enPerfilEnterprise_deberiaEstarProhibido() {
        var env = new MockEnvironment();
        env.setActiveProfiles("enterprise", "prod");

        var sender = new LoggingEmailSender(env);

        assertThrows(IllegalStateException.class, sender::validateNotInProduction);
    }
}
