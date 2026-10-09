package com.kinplatform.license;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "kin-license-generator",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "KIN License Generator - Genera licencias firmadas para clientes On-Premise",
        descriptionHeading = "%nDescripción:%n",
        optionListHeading = "%nOpciones:%n",
        parameterListHeading = "%nParámetros:%n",
        footerHeading = "%nEjemplos:%n",
        footer = {
            "  $ kin-license-generator --org='IPS Salud Total' --product=medical --expires=2027-12-31 --users=50",
            "  $ kin-license-generator --org='Empresa SA' --nit=900123456-7 --product=platform --expires=2028-01-01 --output=licencia.key",
            "  $ kin-license-generator --org='Test IPS' --product=all --expires=2027-12-31 --users=10 --output=test-licencia.key"
        })
public class LicenseGeneratorApp implements Callable<Integer> {

    @Option(names = {"-o", "--org"}, description = "Nombre de la organización (requerido)", required = true)
    String organization;

    @Option(names = {"-n", "--nit"}, description = "NIT de la organización (opcional)")
    String nit;

    @Option(names = {"-p", "--product"}, description = "Producto a licenciar: medical, platform, all (requerido)", required = true)
    String product;

    @Option(names = {"-e", "--expires"}, description = "Fecha de expiración YYYY-MM-DD (requerido)", required = true)
    String expires;

    @Option(names = {"-u", "--users"}, description = "Máximo número de usuarios (default: 10)")
    Integer maxUsers = 10;

    @Option(names = {"-f", "--features"}, description = "Features habilitados separados por coma (ej: hce,rips,mipres,triage)")
    String features;

    @Option(names = {"-x", "--output"}, description = "Archivo de salida (default: licencia.key)")
    String output = "licencia.key";

    @Option(names = {"--private-key"}, description = "Ruta a keystore personalizado (default: ~/.kin/license.keystore)")
    String keyStorePath;

    @Option(names = {"--key-password"}, description = "Contraseña del keystore (default: kin-license-generator)")
    String keyStorePassword;

    @Option(names = {"--export-public-key"}, description = "Exportar clave pública a archivo")
    String exportPublicKey;

    @Option(names = {"--export-private-key"}, description = "Exportar clave privada a archivo (¡cuidado!)")
    String exportPrivateKey;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new LicenseGeneratorApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Validar producto
        String productValue = product.toLowerCase();
        if (!"medical".equals(productValue) && !"platform".equals(productValue) && !"all".equals(productValue)) {
            System.err.println("Error: product debe ser 'medical', 'platform' o 'all'");
            return 1;
        }

        // Parsear features
        List<String> featureList = features != null ?
            Arrays.stream(features.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList() :
            List.of();

        // Parsear fecha de expiración
        Instant expiresAt;
        try {
            expiresAt = LocalDate.parse(expires).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            System.err.println("Error: Formato de fecha inválido. Use YYYY-MM-DD");
            return 1;
        }

        // Verificar que la fecha no sea en el pasado
        if (Instant.now().isAfter(LocalDate.parse(expires).atStartOfDay(ZoneOffset.UTC).toInstant())) {
            System.err.println("Advertencia: La fecha de expiración es en el pasado");
        }

        // Crear LicenseSigner
        LicenseSigner signer;
        if (keyStorePath != null) {
            signer = new LicenseSigner(keyStorePath, keyStorePassword != null ? keyStorePassword : "kin-license-generator");
        } else {
            signer = new LicenseSigner();
        }

        // Exportar claves si se solicita
        if (exportPublicKey != null) {
            try (FileOutputStream fos = new FileOutputStream(exportPublicKey)) {
                signer.exportPublicKey(fos);
                System.out.println("Clave pública exportada a: " + exportPublicKey);
                return 0;
            }
        }

        if (exportPrivateKey != null) {
            try (FileOutputStream fos = new FileOutputStream(exportPrivateKey)) {
                signer.exportPrivateKey(fos);
                System.out.println("¡ADVERTENCIA! Clave privada exportada a: " + exportPrivateKey);
                return 0;
            }
        }

        // Crear payload de licencia
        LicensePayload payload = new LicensePayload();
        payload.setOrganization(organization);
        payload.setNit(nit);
        payload.setProduct(productValue);
        payload.setMaxUsers(maxUsers);
        payload.setFeatures(featureList);
        payload.setExpiresAt(LocalDate.parse(expires).atStartOfDay(ZoneOffset.UTC).toInstant());

        // Firmar licencia
        String signature = signer.sign(payload);
        payload.setSignature(signature);

        // Verificar que la firma es válida
        if (!signer.verify(payload)) {
            System.err.println("Error: La firma generada no es válida");
            return 1;
        }

        // Serializar a JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

        // Escribir archivo de salida
        File outputFile = new File(output);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("Licencia generada exitosamente:");
        System.out.println("  Archivo: " + outputFile.getAbsolutePath());
        System.out.println("  License ID: " + payload.getLicenseId());
        System.out.println("  Organización: " + payload.getOrganization());
        System.out.println("  Producto: " + payload.getProduct());
        System.out.println("  Expira: " + payload.getExpiresAt());
        System.out.println("  Usuarios máx: " + payload.getMaxUsers());
        System.out.println("  Features: " + payload.getFeatures());
        System.out.println("  Firma: " + (payload.getSignature() != null ? "VÁLIDA" : "INVÁLIDA"));

        return 0;
    }
}