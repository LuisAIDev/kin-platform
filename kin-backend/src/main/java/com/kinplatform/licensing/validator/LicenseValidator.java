package com.kinplatform.licensing.validator;

import com.kinplatform.licensing.crypto.LicenseVerifier;
import com.kinplatform.licensing.domain.License;
import com.kinplatform.licensing.domain.LicenseModule;
import com.kinplatform.licensing.domain.LicenseValidationResult;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
public class LicenseValidator {

    private final LicenseVerifier verifier;
    private final ServerHashCalculator serverHashCalculator;

    public LicenseValidator(LicenseVerifier verifier,
                            ServerHashCalculator serverHashCalculator) {
        this.verifier = verifier;
        this.serverHashCalculator = serverHashCalculator;
    }

    public LicenseValidationResult validate(String licenseFileContent,
                                             PublicKey publicKey,
                                             int currentPhysicianCount) {
        try {
            ParsedLicense parsed = parseLicenseFile(licenseFileContent);
            if (parsed == null) {
                return LicenseValidationResult.invalid("Formato de licencia inválido");
            }

            if (!verifier.verify(parsed.plainText(), parsed.signature(), publicKey)) {
                return LicenseValidationResult.invalid("Firma digital inválida");
            }

            License license = parseFields(parsed.plainText());
            if (license == null) {
                return LicenseValidationResult.invalid("Campos de licencia inválidos");
            }

            if (license.isExpired()) {
                return LicenseValidationResult.expired("Licencia expirada el " + license.getExpiresAt());
            }

            String currentHash = serverHashCalculator.calculate();
            if (!currentHash.equals(license.getServerHash())) {
                return LicenseValidationResult.mismatch("Esta licencia no corresponde a este servidor");
            }

            if (currentPhysicianCount > license.getMaxPhysicians()) {
                return LicenseValidationResult.invalid(
                        "Médicos activos (" + currentPhysicianCount + ") exceden el límite ("
                                + license.getMaxPhysicians() + ")");
            }

            return LicenseValidationResult.active(license, "Licencia válida");
        } catch (Exception e) {
            return LicenseValidationResult.invalid("Error validando licencia: " + e.getMessage());
        }
    }

    private ParsedLicense parseLicenseFile(String content) {
        if (content == null || !content.contains("-----BEGIN KIN LICENSE-----")) {
            return null;
        }
        try {
            String between = content
                    .replace("-----BEGIN KIN LICENSE-----", "")
                    .replace("-----END KIN LICENSE-----", "")
                    .trim();

            int sigStart = between.indexOf("-----BEGIN SIGNATURE-----");
            int sigEnd = between.indexOf("-----END SIGNATURE-----");

            String plainText = between.substring(0, sigStart);
            String signature = between.substring(sigStart + "-----BEGIN SIGNATURE-----".length(), sigEnd).trim();

            return new ParsedLicense(plainText, signature);
        } catch (Exception e) {
            return null;
        }
    }

    private License parseFields(String plainText) {
        try {
            String licenseId = null, ipsName = null, nit = null, serverHash = null;
            Instant issuedAt = null, expiresAt = null;
            int maxPhysicians = 0;
            Set<LicenseModule> modules = EnumSet.noneOf(LicenseModule.class);

            for (String line : plainText.split("\n")) {
                String[] parts = line.split(": ", 2);
                if (parts.length < 2) continue;
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "LICENSE_ID"      -> licenseId = value;
                    case "IPS_NAME"        -> ipsName = value;
                    case "NIT"             -> nit = value;
                    case "SERVER_HASH"     -> serverHash = value;
                    case "ISSUED_AT"       -> issuedAt = Instant.parse(value);
                    case "EXPIRES_AT"      -> expiresAt = Instant.parse(value);
                    case "MAX_PHYSICIANS"  -> maxPhysicians = Integer.parseInt(value);
                    case "MODULES"         -> {
                        for (String m : value.split(",")) {
                            modules.add(LicenseModule.fromString(m));
                        }
                    }
                }
            }

            return new License(licenseId, ipsName, nit, serverHash,
                    issuedAt, expiresAt, maxPhysicians, modules);
        } catch (Exception e) {
            return null;
        }
    }

    private record ParsedLicense(String plainText, String signature) { }
}