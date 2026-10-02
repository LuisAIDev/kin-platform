package com.kinplatform.licensing.crypto;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class LicenseSigner {

    private static final String ALGORITHM = "SHA256withRSA";

    public String sign(String plainText, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getMimeEncoder(76, "\n".getBytes()).encodeToString(signed);
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando licencia: " + e.getMessage(), e);
        }
    }
}
