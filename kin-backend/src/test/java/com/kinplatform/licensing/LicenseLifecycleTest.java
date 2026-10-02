package com.kinplatform.licensing;

import com.kinplatform.licensing.crypto.KeyPairLoader;
import com.kinplatform.licensing.crypto.LicenseSigner;
import com.kinplatform.licensing.crypto.LicenseVerifier;
import com.kinplatform.licensing.domain.License;
import com.kinplatform.licensing.domain.LicenseModule;
import com.kinplatform.licensing.domain.LicenseValidationResult;
import com.kinplatform.licensing.generator.LicenseGenerator;
import com.kinplatform.licensing.service.LicenseService;
import com.kinplatform.licensing.validator.LicenseValidator;
import com.kinplatform.licensing.validator.ServerHashCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ciclo de vida completo de una licencia a nivel de componente (SIN levantar un
 * segundo contexto Spring): generar -> persistir a disco -> cargar desde disco
 * con {@link LicenseService#loadLicenseOnStartup()}.
 *
 * <p>Usa un par RSA FIJO de test en {@code src/test/resources/license-keys/}
 * (kin-test-private.pem / kin-test-public.pem), NUNCA el par real de
 * {@code kin-backend/config/license-keys/} (gitignored). Se evita el nombre
 * {@code kin-public.pem} para no sombrear la clave publica de produccion del
 * classpath usada por el constructor de {@code LicenseService}.</p>
 *
 * <p><b>Nota de despliegue (no es bug; pendiente documentar en Fase D):</b>
 * el endpoint {@code POST /admin/licensing/generate} NO persiste la licencia a
 * disco automaticamente: devuelve el contenido en el cuerpo HTTP y el operador
 * debe guardarlo manualmente en la ruta {@code kin.license.file-path}. Este test
 * persiste el archivo explicitamente para cubrir el ciclo real.</p>
 */
class LicenseLifecycleTest {

    private static final String TEST_PRIVATE_KEY = "license-keys/kin-test-private.pem";
    private static final String TEST_PUBLIC_KEY = "license-keys/kin-test-public.pem";

    @Test
    void generaPersisteYCargaDesdeDisco(@TempDir Path tempDir) throws Exception {
        // 1. Par de claves de test (real, cargado por KeyPairLoader)
        KeyPairLoader keyPairLoader = new KeyPairLoader();
        Path privateKeyPath = new ClassPathResource(TEST_PRIVATE_KEY).getFile().toPath();
        PrivateKey privateKey = keyPairLoader.loadPrivateKey(privateKeyPath);

        // 2. Generar la licencia firmada (serverHash debe coincidir con este host)
        String serverHash = new ServerHashCalculator().calculate();
        Set<LicenseModule> modules = EnumSet.of(LicenseModule.HCE, LicenseModule.TRIAJE);
        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                "LIC-2026-7777",
                "IPS Lifecycle Test",
                "901222333-4",
                serverHash,
                10,
                modules,
                privateKey
        );

        // 3. Persistir la licencia a un archivo REAL en disco (@TempDir)
        Path licenseFile = tempDir.resolve("license.key");
        Files.writeString(licenseFile, licenseContent);
        assertTrue(Files.exists(licenseFile), "La licencia debe existir en disco");

        // 4. Instanciar LicenseService apuntando al archivo persistido
        Resource publicKeyResource = new ClassPathResource(TEST_PUBLIC_KEY);
        LicenseService service = new LicenseService(
                new LicenseValidator(new LicenseVerifier(), new ServerHashCalculator()),
                keyPairLoader,
                publicKeyResource
        );
        ReflectionTestUtils.setField(service, "licenseFilePath", licenseFile.toString());

        // 5. Cargar desde disco (equivalente al @PostConstruct de arranque)
        service.loadLicenseOnStartup();

        // 6. Verificar licencia activa y datos correctos
        License loaded = service.getCurrentLicense();
        assertNotNull(loaded, "LicenseService debe cargar la licencia persistida desde disco");
        assertTrue(service.getLastValidation().isActive(), "La validacion debe quedar ACTIVE");
        assertEquals("LIC-2026-7777", loaded.getLicenseId());
        assertEquals("IPS Lifecycle Test", loaded.getIpsName());
        assertEquals("901222333-4", loaded.getNit());
        assertEquals(10, loaded.getMaxPhysicians());
        assertTrue(loaded.isModuleEnabled(LicenseModule.HCE));
        assertTrue(loaded.isModuleEnabled(LicenseModule.TRIAJE));
        assertFalse(loaded.isModuleEnabled(LicenseModule.OCR));
        assertFalse(loaded.isExpired());
        assertTrue(service.isModuleEnabled(LicenseModule.HCE));
        assertEquals(10, service.getRemainingPhysicianSlots(0));
    }

    @Test
    void licenciaConFirmaInvalidaNoActiva(@TempDir Path tempDir) throws Exception {
        // 1. Par de claves de test (real, cargado por KeyPairLoader)
        KeyPairLoader keyPairLoader = new KeyPairLoader();
        Path privateKeyPath = new ClassPathResource(TEST_PRIVATE_KEY).getFile().toPath();
        PrivateKey privateKey = keyPairLoader.loadPrivateKey(privateKeyPath);

        // 2. Generar una licencia valida y luego alterar el contenido firmado
        String serverHash = new ServerHashCalculator().calculate();
        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                "LIC-2026-0009",
                "IPS Bad S.A.S.",
                "900000000-0",
                serverHash,
                5,
                EnumSet.of(LicenseModule.HCE),
                privateKey
        );

        // 3. Alterar el contenido -> la firma deja de ser valida
        String tampered = licenseContent.replace("IPS Bad S.A.S.", "IPS Hacked S.A.S.");

        // 4. Persistir el contenido alterado a disco
        Path licenseFile = tempDir.resolve("license.key");
        Files.writeString(licenseFile, tampered);

        // 5. Instanciar LicenseService (fail-fast=false: no lanza, queda null)
        LicenseService service = new LicenseService(
                new LicenseValidator(new LicenseVerifier(), new ServerHashCalculator()),
                keyPairLoader,
                new ClassPathResource(TEST_PUBLIC_KEY)
        );
        ReflectionTestUtils.setField(service, "licenseFilePath", licenseFile.toString());
        ReflectionTestUtils.setField(service, "failFast", false);

        // 6. Cargar desde disco (equivalente al @PostConstruct de arranque)
        service.loadLicenseOnStartup();

        // 7. Verificar rechazo por firma invalida
        assertNull(service.getCurrentLicense(), "Una licencia con firma invalida no debe quedar activa");
        assertNotNull(service.getLastValidation());
        assertFalse(service.getLastValidation().isActive());
        assertEquals(LicenseValidationResult.Status.INVALID, service.getLastValidation().getStatus());
    }
}