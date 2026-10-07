package com.kinplatform.kin.medical.licensing.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class License {

    private final String licenseId;
    private final String ipsName;
    private final String nit;
    private final String serverHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final int maxPhysicians;
    private final Set<LicenseModule> modules;

    public License(String licenseId, String ipsName, String nit, String serverHash,
                   Instant issuedAt, Instant expiresAt, int maxPhysicians,
                   Set<LicenseModule> modules) {
        if (licenseId == null || !licenseId.matches("LIC-\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException("licenseId inválido: " + licenseId);
        }
        if (ipsName == null || ipsName.isBlank() || ipsName.length() > 200) {
            throw new IllegalArgumentException("ipsName inválido");
        }
        if (nit == null || nit.isBlank()) {
            throw new IllegalArgumentException("nit inválido");
        }
        if (serverHash == null || serverHash.length() != 64) {
            throw new IllegalArgumentException("serverHash debe ser SHA-256 (64 chars)");
        }
        if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Fechas inválidas");
        }
        if (maxPhysicians < 1 || maxPhysicians > 10_000) {
            throw new IllegalArgumentException("maxPhysicians fuera de rango");
        }
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("Debe tener al menos 1 módulo");
        }

        this.licenseId = licenseId;
        this.ipsName = ipsName;
        this.nit = nit;
        this.serverHash = serverHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.maxPhysicians = maxPhysicians;
        this.modules = Collections.unmodifiableSet(EnumSet.copyOf(modules));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isModuleEnabled(LicenseModule module) {
        return modules.contains(module);
    }

    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        sb.append("KIN-LICENSE-v1\n");
        sb.append("LICENSE_ID: ").append(licenseId).append("\n");
        sb.append("IPS_NAME: ").append(ipsName).append("\n");
        sb.append("NIT: ").append(nit).append("\n");
        sb.append("SERVER_HASH: ").append(serverHash).append("\n");
        sb.append("ISSUED_AT: ").append(issuedAt).append("\n");
        sb.append("EXPIRES_AT: ").append(expiresAt).append("\n");
        sb.append("MAX_PHYSICIANS: ").append(maxPhysicians).append("\n");
        sb.append("MODULES: ");
        sb.append(String.join(",", modules.stream().map(Enum::name).toList()));
        sb.append("\n");
        return sb.toString();
    }

    public String getLicenseId() { return licenseId; }
    public String getIpsName() { return ipsName; }
    public String getNit() { return nit; }
    public String getServerHash() { return serverHash; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getMaxPhysicians() { return maxPhysicians; }
    public Set<LicenseModule> getModules() { return modules; }
}

