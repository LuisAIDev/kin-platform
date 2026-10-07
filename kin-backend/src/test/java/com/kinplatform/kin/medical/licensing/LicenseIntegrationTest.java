package com.kinplatform.kin.medical.licensing;

import com.kinplatform.kin.medical.licensing.crypto.LicenseSigner;
import com.kinplatform.kin.medical.licensing.crypto.LicenseVerifier;
import com.kinplatform.kin.medical.licensing.domain.License;
import com.kinplatform.kin.medical.licensing.domain.LicenseModule;
import com.kinplatform.kin.medical.licensing.domain.LicenseValidationResult;
import com.kinplatform.kin.medical.licensing.generator.LicenseGenerator;
import com.kinplatform.kin.medical.licensing.validator.LicenseValidator;
import com.kinplatform.kin.medical.licensing.validator.ServerHashCalculator;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LicenseIntegrationTest {

    @Test
    void cicloCompletoGeneracionYValidacion() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        String serverHash = new ServerHashCalculator().calculate();
        Set<LicenseModule> modules = EnumSet.of(LicenseModule.HCE, LicenseModule.TRIAJE);

        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                "LIC-2026-0001",
                "Clinica Test S.A.S.",
                "900123456-7",
                serverHash,
                10,
                modules,
                keyPair.getPrivate()
        );

        LicenseValidator validator = new LicenseValidator(
                new LicenseVerifier(),
                new ServerHashCalculator()
        );

        LicenseValidationResult result = validator.validate(
                licenseContent,
                keyPair.getPublic(),
                5
        );

        assertTrue(result.isActive(), "La licencia debe ser válida");
        License license = result.getLicense();
        assertNotNull(license);
        assertEquals("LIC-2026-0001", license.getLicenseId());
        assertEquals("Clinica Test S.A.S.", license.getIpsName());
        assertEquals(10, license.getMaxPhysicians());
        assertTrue(license.isModuleEnabled(LicenseModule.HCE));
        assertTrue(license.isModuleEnabled(LicenseModule.TRIAJE));
        assertFalse(license.isModuleEnabled(LicenseModule.OCR));
    }

    @Test
    void licenciaExcedeMedicosFallaValidacion() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        String serverHash = new ServerHashCalculator().calculate();
        Set<LicenseModule> modules = EnumSet.of(LicenseModule.HCE);

        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                "LIC-2026-0002", "Test S.A.S.", "900123456-7",
                serverHash, 10, modules, keyPair.getPrivate()
        );

        LicenseValidator validator = new LicenseValidator(
                new LicenseVerifier(),
                new ServerHashCalculator()
        );

        LicenseValidationResult result = validator.validate(
                licenseContent, keyPair.getPublic(), 15
        );

        assertFalse(result.isActive());
        assertEquals(LicenseValidationResult.Status.INVALID, result.getStatus());
    }

    @Test
    void licenciaServidorEquivocadoFalla() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        Set<LicenseModule> modules = EnumSet.of(LicenseModule.HCE);

        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                "LIC-2026-0003", "Test S.A.S.", "900123456-7",
                "0000000000000000000000000000000000000000000000000000000000000000",
                10, modules, keyPair.getPrivate()
        );

        LicenseValidator validator = new LicenseValidator(
                new LicenseVerifier(),
                new ServerHashCalculator()
        );

        LicenseValidationResult result = validator.validate(
                licenseContent, keyPair.getPublic(), 5
        );

        assertFalse(result.isActive());
        assertEquals(LicenseValidationResult.Status.MISMATCH, result.getStatus());
    }

    @Test
    void licenciaModificadaFallaVerificacion() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        String serverHash = new ServerHashCalculator().calculate();
        Set<LicenseModule> modules = EnumSet.of(LicenseModule.HCE);

        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                "LIC-2026-0004", "Test S.A.S.", "900123456-7",
                serverHash, 10, modules, keyPair.getPrivate()
        );

        String tampered = licenseContent.replace("Test S.A.S.", "Hack S.A.S.");

        LicenseValidator validator = new LicenseValidator(
                new LicenseVerifier(),
                new ServerHashCalculator()
        );

        LicenseValidationResult result = validator.validate(
                tampered, keyPair.getPublic(), 5
        );

        assertFalse(result.isActive());
        assertEquals(LicenseValidationResult.Status.INVALID, result.getStatus());
    }
}

