package com.kinplatform.kin.medical.licensing.controller;

import com.kinplatform.kin.medical.licensing.crypto.KeyPairLoader;
import com.kinplatform.kin.medical.licensing.domain.License;
import com.kinplatform.kin.medical.licensing.domain.LicenseModule;
import com.kinplatform.kin.medical.licensing.generator.LicenseGenerator;
import com.kinplatform.kin.medical.licensing.service.LicenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/licensing")
public class LicenseAdminController {

    private static final Logger log = LoggerFactory.getLogger(LicenseAdminController.class);

    private final LicenseGenerator generator;
    private final LicenseService licenseService;
    private final KeyPairLoader keyPairLoader;

    @Value("${kin.license.private-key-path}")
    private String privateKeyPath;

    public LicenseAdminController(LicenseGenerator generator,
                                   LicenseService licenseService,
                                   KeyPairLoader keyPairLoader) {
        this.generator = generator;
        this.licenseService = licenseService;
        this.keyPairLoader = keyPairLoader;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generateLicense(@RequestBody Map<String, Object> body) {
        try {
            String licenseId = (String) body.get("licenseId");
            String ipsName = (String) body.get("ipsName");
            String nit = (String) body.get("nit");
            String serverHash = (String) body.get("serverHash");
            int maxPhysicians = (int) body.getOrDefault("maxPhysicians", 10);

            @SuppressWarnings("unchecked")
            Set<LicenseModule> modules = EnumSet.noneOf(LicenseModule.class);
            Object modulesObj = body.get("modules");
            if (modulesObj instanceof Iterable<?> iterable) {
                for (Object m : iterable) {
                    modules.add(LicenseModule.fromString(m.toString()));
                }
            }

            PrivateKey privateKey = keyPairLoader.loadPrivateKey(Path.of(privateKeyPath));

            String licenseContent = generator.generate(
                    licenseId, ipsName, nit, serverHash,
                    maxPhysicians, modules, privateKey);

            log.info("Licencia generada: {} para {}", licenseId, ipsName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + licenseId + ".key")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(licenseContent.getBytes());

        } catch (Exception e) {
            log.error("Error generando licencia: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'IPS_ADMIN')")
    public ResponseEntity<Map<String, Object>> status() {
        License license = licenseService.getCurrentLicense();
        if (license == null) {
            return ResponseEntity.ok(Map.of(
                    "active", false,
                    "message", "Sin licencia activa"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "active", true,
                "licenseId", license.getLicenseId(),
                "ipsName", license.getIpsName(),
                "expiresAt", license.getExpiresAt().toString(),
                "maxPhysicians", license.getMaxPhysicians(),
                "modules", license.getModules().stream().map(Enum::name).toList()
        ));
    }

    @PostMapping("/reload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reload() {
        try {
            licenseService.loadLicenseOnStartup();
            return ResponseEntity.ok(Map.of("status", "reloaded"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

