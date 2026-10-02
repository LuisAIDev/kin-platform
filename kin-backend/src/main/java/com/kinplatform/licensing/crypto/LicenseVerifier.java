package com.kinplatform.licensing.crypto;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class LicenseVerifier {

    private static final String ALGORITHM = "SHA256withRSA";

    public boolean verify(String plainText, String signatureBase64, PublicKey publicKey) {
        try {
            String cleanSignature = signatureBase64.replaceAll("\\s", "");
            byte[] signatureBytes = Base64.getDecoder().decode(cleanSignature);

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(plainText.getBytes(StandardCharsets.UTF_8));

            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
