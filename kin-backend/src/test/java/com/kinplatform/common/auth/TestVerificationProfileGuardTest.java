package com.kinplatform.common.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/**
 * Verificación documental de la anotación de perfil del test hook: debe ser
 * EXCLUSIVAMENTE {@code test} (nunca prod/dev/render/enterprise).
 */
class TestVerificationProfileGuardTest {

    @Test
    void anotacionDePerfil_deberiaSerExclusivamenteTest() {
        Profile profile = TestVerificationController.class.getAnnotation(Profile.class);
        assertNotNull(profile);
        var values = Arrays.asList(profile.value());
        assertTrue(values.contains("test"));
        assertTrue(!values.contains("prod"));
        assertTrue(!values.contains("dev"));
        assertTrue(!values.contains("render"));
        assertTrue(!values.contains("enterprise"));
    }
}

