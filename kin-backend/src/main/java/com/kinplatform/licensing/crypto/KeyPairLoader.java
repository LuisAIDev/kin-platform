package com.kinplatform.licensing.crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class KeyPairLoader {

    public PrivateKey loadPrivateKey(Path path) {
        try {
            String pem = Files.readString(path);
            String base64 = extractBase64(pem, "PRIVATE KEY");
            byte[] decoded = Base64.getDecoder().decode(base64);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando clave privada: " + e.getMessage(), e);
        }
    }

    public PublicKey loadPublicKey(String pemContent) {
        try {
            String base64 = extractBase64(pemContent, "PUBLIC KEY");
            byte[] decoded = Base64.getDecoder().decode(base64);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando clave pública: " + e.getMessage(), e);
        }
    }

    private String extractBase64(String pem, String type) {
        return pem.replace("-----BEGIN " + type + "-----", "")
                  .replace("-----END " + type + "-----", "")
                  .replaceAll("\\s", "");
    }
}