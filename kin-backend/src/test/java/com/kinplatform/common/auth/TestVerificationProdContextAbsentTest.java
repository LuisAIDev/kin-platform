package com.kinplatform.common.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Con perfil {@code prod}, el test hook y su store NO deben existir en el
 * contexto de Spring: un deploy de producción no expone
 * {@code /auth/test/verification-link}.
 */
@SpringJUnitConfig(classes = {TestVerificationController.class, TestVerificationStore.class})
@ActiveProfiles("prod")
class TestVerificationProdContextAbsentTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void conPerfilProd_noDeberiaExistirElControllerNiElStore() {
        assertTrue(context.getBeansOfType(TestVerificationController.class).isEmpty());
        assertTrue(context.getBeansOfType(TestVerificationStore.class).isEmpty());
    }
}

