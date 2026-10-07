package com.kinplatform.kin.medical.licensing.generator;

import com.kinplatform.kin.medical.licensing.crypto.LicenseSigner;
import com.kinplatform.kin.medical.licensing.domain.License;
import com.kinplatform.kin.medical.licensing.domain.LicenseModule;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
public class LicenseGenerator {

    private final LicenseSigner signer;

    public LicenseGenerator(LicenseSigner signer) {
        this.signer = signer;
    }

    public String generate(String licenseId,
                           String ipsName,
                           String nit,
                           String serverHash,
                           int maxPhysicians,
                           Set<LicenseModule> modules,
                           PrivateKey privateKey) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(365, ChronoUnit.DAYS);

        License license = new License(
                licenseId, ipsName, nit, serverHash,
                issuedAt, expiresAt, maxPhysicians, modules
        );

        String plainText = license.toPlainText();
        String signature = signer.sign(plainText, privateKey);

        return formatLicenseFile(plainText, signature);
    }

    private String formatLicenseFile(String plainText, String signature) {
        return "-----BEGIN KIN LICENSE-----\n"
                + plainText
                + "-----BEGIN SIGNATURE-----\n"
                + signature + "\n"
                + "-----END SIGNATURE-----\n"
                + "-----END KIN LICENSE-----\n";
    }
}

