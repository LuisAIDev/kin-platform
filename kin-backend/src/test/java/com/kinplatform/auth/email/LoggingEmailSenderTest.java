package com.kinplatform.auth.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kinplatform.auth.TestVerificationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class LoggingEmailSenderTest {

    private static LoggingEmailSender senderWith(MockEnvironment env, DefaultListableBeanFactory bf) {
        return new LoggingEmailSender(env, bf.getBeanProvider(TestVerificationStore.class));
    }

    @Test
    void enPerfilDev_noDeberiaFallar() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");

        var sender = senderWith(env, new DefaultListableBeanFactory());

        assertDoesNotThrow(sender::validateNotInProduction);
    }

    @Test
    void enPerfilProd_deberiaEstarProhibido() {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");

        var sender = senderWith(env, new DefaultListableBeanFactory());

        assertThrows(IllegalStateException.class, sender::validateNotInProduction);
    }

    @Test
    void enPerfilRender_deberiaEstarProhibido() {
        var env = new MockEnvironment();
        env.setActiveProfiles("render", "prod");

        var sender = senderWith(env, new DefaultListableBeanFactory());

        assertThrows(IllegalStateException.class, sender::validateNotInProduction);
    }

    @Test
    void enPerfilEnterprise_deberiaEstarProhibido() {
        var env = new MockEnvironment();
        env.setActiveProfiles("enterprise", "prod");

        var sender = senderWith(env, new DefaultListableBeanFactory());

        assertThrows(IllegalStateException.class, sender::validateNotInProduction);
    }

    @Test
    void conStoreDisponible_deberiaCapturarElEnlace() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");
        var store = new TestVerificationStore();
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        bf.registerSingleton("store", store);

        var sender = senderWith(env, bf);
        sender.sendVerificationEmail("a@kin.test", "Ana", "https://kin.test/verify-email?token=tok123");

        assertEquals("https://kin.test/verify-email?token=tok123", store.get("a@kin.test"));
    }

    @Test
    void sinStoreDisponible_noDeberiaFallarNiCapturar() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");

        var sender = senderWith(env, new DefaultListableBeanFactory());

        assertDoesNotThrow(() -> sender.sendVerificationEmail("a@kin.test", "Ana", "https://kin.test/link"));
    }
}
