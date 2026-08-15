package com.kinplatform.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Con perfil {@code test}, el test hook y su store DEBEN existir en el
 * contexto de Spring (para que Playwright pueda recuperar el enlace).
 */
@SpringJUnitConfig(classes = {TestVerificationController.class, TestVerificationStore.class})
@ActiveProfiles("test")
class TestVerificationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void conPerfilTest_deberiaExistirElControllerYElStore() {
        assertTrue(!context.getBeansOfType(TestVerificationController.class).isEmpty());
        assertTrue(!context.getBeansOfType(TestVerificationStore.class).isEmpty());
    }
}
