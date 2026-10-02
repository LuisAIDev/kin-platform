package com.kinplatform.licensing;

import com.kinplatform.licensing.crypto.LicenseSigner;
import com.kinplatform.licensing.crypto.LicenseVerifier;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

class LicenseSignerTest {

    @Test
    void firmaYVerificacionFuncionan() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        String contenido = "KIN-LICENSE-v1\nIPS: Test\n";

        LicenseSigner signer = new LicenseSigner();
        LicenseVerifier verifier = new LicenseVerifier();

        String firma = signer.sign(contenido, keyPair.getPrivate());

        assertTrue(verifier.verify(contenido, firma, keyPair.getPublic()),
                "La firma debe ser válida");
    }

    @Test
    void firmaFallaSiContenidoCambia() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        LicenseSigner signer = new LicenseSigner();
        LicenseVerifier verifier = new LicenseVerifier();

        String firma = signer.sign("contenido original", keyPair.getPrivate());

        assertFalse(verifier.verify("contenido modificado", firma, keyPair.getPublic()),
                "La firma debe fallar si el contenido cambió");
    }
}
