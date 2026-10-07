package com.kinplatform.kin.medical.billing.fev;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Firma XML simulada para desarrollo y tests.
 *
 * NO usar en produccion: no aplica certificado X.509 ni clave privada real.
 * La implementacion productiva (HSM / AWS KMS) vive en {@link KmsXmlSigner}.
 */
@Component
@Profile("!prod")
@Slf4j
public class StubXmlSigner implements XmlSigner {

    @Override
    public Signature sign(String xml) {
        if (xml == null) {
            throw new IllegalArgumentException("XML a firmar no puede ser null");
        }
        String cufe = sha256Hex(xml);
        String qrCode = "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + cufe;
        String signedXml = xml + "\n<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
                + "<ds:SignedInfo><ds:DigestValue>" + cufe + "</ds:DigestValue></ds:SignedInfo>"
                + "<ds:SignatureValue>STUB-" + cufe.substring(0, 16).toUpperCase() + "</ds:SignatureValue>"
                + "</ds:Signature>";
        log.debug("XML firmado (STUB) cufe={}", cufe);
        return new Signature(signedXml, cufe, qrCode);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

