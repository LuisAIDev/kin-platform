package com.kinplatform.kin.medical.licensing.service;

import com.kinplatform.kin.medical.licensing.crypto.KeyPairLoader;
import com.kinplatform.kin.medical.licensing.domain.License;
import com.kinplatform.kin.medical.licensing.domain.LicenseModule;
import com.kinplatform.kin.medical.licensing.domain.LicenseValidationResult;
import com.kinplatform.kin.medical.licensing.validator.LicenseValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;

@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    @Value("${kin.license.file-path:/opt/kin/config/license.key}")
    private String licenseFilePath;

    @Value("${kin.license.fail-fast:false}")
    private boolean failFast;

    private final LicenseValidator validator;
    private final KeyPairLoader keyPairLoader;
    private final Resource publicKeyResource;

    private License currentLicense;
    private LicenseValidationResult lastValidation;

    public LicenseService(LicenseValidator validator,
                          KeyPairLoader keyPairLoader,
                          @Value("classpath:license-keys/kin-public.pem") Resource publicKeyResource) {
        this.validator = validator;
        this.keyPairLoader = keyPairLoader;
        this.publicKeyResource = publicKeyResource;
    }

    @PostConstruct
    public void loadLicenseOnStartup() {
        log.info("Cargando licencia KIN desde: {}", licenseFilePath);

        try {
            Path path = Path.of(licenseFilePath);
            if (!Files.exists(path)) {
                handleFailure("Archivo de licencia no encontrado: " + licenseFilePath);
                return;
            }

            String content = Files.readString(path);
            String pubKeyContent = new String(publicKeyResource.getInputStream().readAllBytes());
            PublicKey publicKey = keyPairLoader.loadPublicKey(pubKeyContent);

            int currentPhysicianCount = 0;

            LicenseValidationResult result = validator.validate(content, publicKey, currentPhysicianCount);
            this.lastValidation = result;

            if (result.isActive()) {
                this.currentLicense = result.getLicense();
                log.info("Licencia válida para: {} (expira: {})",
                        currentLicense.getIpsName(),
                        currentLicense.getExpiresAt());
            } else {
                handleFailure("Licencia inválida: " + result.getMessage());
            }
        } catch (Exception e) {
            handleFailure("Error cargando licencia: " + e.getMessage());
        }
    }

    private void handleFailure(String message) {
        log.error("Error de licencia: {}", message);
        if (failFast) {
            throw new IllegalStateException(message);
        } else {
            log.warn("KIN arranca en modo sin licencia (fail-fast=false)");
        }
    }

    public boolean isModuleEnabled(LicenseModule module) {
        return currentLicense != null && currentLicense.isModuleEnabled(module);
    }

    public License getCurrentLicense() {
        return currentLicense;
    }

    public LicenseValidationResult getLastValidation() {
        return lastValidation;
    }

    public int getRemainingPhysicianSlots(int currentPhysicianCount) {
        if (currentLicense == null) return 0;
        return Math.max(0, currentLicense.getMaxPhysicians() - currentPhysicianCount);
    }
}

