package com.kinplatform.licensing.tools;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class GenerateKeys {

    public static void main(String[] args) throws Exception {
        String outputDir = args.length > 0 ? args[0] : "config/license-keys";

        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(4096);
        KeyPair keyPair = gen.generateKeyPair();

        String privatePem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        Path privatePath = dir.resolve("kin-private.pem");
        Files.writeString(privatePath, privatePem, StandardCharsets.UTF_8);
        System.out.println("Private key: " + privatePath.toAbsolutePath() + " (" + privatePem.length() + " chars)");

        String publicPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
        Path publicPath = dir.resolve("kin-public.pem");
        Files.writeString(publicPath, publicPem, StandardCharsets.UTF_8);
        System.out.println("Public key: " + publicPath.toAbsolutePath() + " (" + publicPem.length() + " chars)");
    }

    private static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}