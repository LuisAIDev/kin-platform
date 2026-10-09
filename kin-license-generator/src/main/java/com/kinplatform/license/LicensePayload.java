package com.kinplatform.license;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicensePayload {

    @JsonProperty("licenseId")
    private String licenseId;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("nit")
    private String nit;

    @JsonProperty("product")
    private String product;

    @JsonProperty("issuedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant issuedAt;

    @JsonProperty("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiresAt;

    @JsonProperty("maxUsers")
    private Integer maxUsers;

    @JsonProperty("features")
    private List<String> features;

    @JsonProperty("signature")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private String signature;

    public LicensePayload() {
        this.licenseId = UUID.randomUUID().toString();
        this.issuedAt = Instant.now();
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Returns a copy of this payload without the signature field (for signing).
     */
    public LicensePayload withoutSignature() {
        LicensePayload copy = new LicensePayload();
        copy.licenseId = this.licenseId;
        copy.organization = this.organization;
        copy.nit = this.nit;
        copy.product = this.product;
        copy.issuedAt = this.issuedAt;
        copy.expiresAt = this.expiresAt;
        copy.maxUsers = this.maxUsers;
        copy.features = this.features;
        copy.signature = null;
        return copy;
    }

    /**
     * Checks if the license is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the license is valid for the given product.
     */
    public boolean isValidForProduct(String product) {
        if (this.product == null || "all".equalsIgnoreCase(this.product)) {
            return true;
        }
        return this.product.equalsIgnoreCase(product);
    }
}