package com.kinplatform.licensing.tools;

import com.kinplatform.licensing.crypto.KeyPairLoader;
import com.kinplatform.licensing.crypto.LicenseSigner;
import com.kinplatform.licensing.domain.LicenseModule;
import com.kinplatform.licensing.generator.LicenseGenerator;
import com.kinplatform.licensing.validator.ServerHashCalculator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.EnumSet;
import java.util.Set;

/**
 * Tool de desarrollo para generar licencias .key reales.
 *
 * Uso:
 *   java -cp target/test-classes:target/classes \
 *        com.kinplatform.licensing.tools.GenerateLicense \
 *        <licenseId> <ipsName> <nit> <maxPhysicians> <modules-csv>
 *
 * Ejemplo:
 *   java -cp target/test-classes:target/classes \
 *        com.kinplatform.licensing.tools.GenerateLicense \
 *        LIC-2026-0001 "IPS Demo S.A.S." 900123456-7 5 HCE,TRIAJE
 *
 * Salida: config/license.key
 *
 * IMPORTANTE: este tool es de desarrollo. En producción, la generación
 * se hace vía POST /admin/licensing/generate (LicenseAdminController).
 */
public class GenerateLicense {

    private static final String PRIVATE_KEY_PATH = "config/license-keys/kin-private.pem";
    private static final String OUTPUT_PATH = "config/license.key";

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Uso: GenerateLicense <licenseId> <ipsName> <nit> <maxPhysicians> <modules-csv>");
            System.err.println("Ejemplo: GenerateLicense LIC-2026-0001 \"IPS Demo S.A.S.\" 900123456-7 5 HCE,TRIAJE");
            System.exit(1);
        }

        String licenseId = args[0];
        String ipsName = args[1];
        String nit = args[2];
        int maxPhysicians = Integer.parseInt(args[3]);
        Set<LicenseModule> modules = EnumSet.noneOf(LicenseModule.class);
        for (String m : args[4].split(",")) {
            modules.add(LicenseModule.fromString(m.trim()));
        }

        System.out.println("=== Generador de licencias KIN ===");
        System.out.println("License ID: " + licenseId);
        System.out.println("IPS: " + ipsName);
        System.out.println("NIT: " + nit);
        System.out.println("Máx médicos: " + maxPhysicians);
        System.out.println("Módulos: " + modules);

        // 1. Cargar clave privada real
        Path privateKeyPath = Path.of(PRIVATE_KEY_PATH);
        if (!Files.exists(privateKeyPath)) {
            System.err.println("ERROR: No existe " + privateKeyPath.toAbsolutePath());
            System.err.println("Debes tener el par de claves reales en config/license-keys/");
            System.exit(1);
        }

        KeyPairLoader loader = new KeyPairLoader();
        PrivateKey privateKey = loader.loadPrivateKey(privateKeyPath);
        System.out.println("✓ Clave privada cargada: " + privateKeyPath.toAbsolutePath());

        // 2. Calcular server hash de la máquina actual
        String serverHash = new ServerHashCalculator().calculate();
        System.out.println("✓ Server hash (esta máquina): " + serverHash);

        // 3. Generar licencia
        LicenseGenerator generator = new LicenseGenerator(new LicenseSigner());
        String licenseContent = generator.generate(
                licenseId, ipsName, nit, serverHash,
                maxPhysicians, modules, privateKey
        );
        System.out.println("✓ Licencia firmada (RSA 4096-bit SHA256)");
        System.out.println("  Tamaño: " + licenseContent.length() + " chars");

        // 4. Escribir a disco
        Path outputPath = Path.of(OUTPUT_PATH);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, licenseContent);
        System.out.println("✓ Archivo generado: " + outputPath.toAbsolutePath());
        System.out.println("");
        System.out.println("Próximo paso:");
        System.out.println("  1. Copiar " + OUTPUT_PATH + " a la IPS");
        System.out.println("  2. Configurar KIN_LICENSE_PATH en la IPS");
        System.out.println("  3. Configurar KIN_LICENSE_FAIL_FAST=true");
        System.out.println("  4. Reiniciar KIN");
    }
}