package com.kinplatform.licensing.domain;

public class LicenseValidationResult {

    public enum Status { ACTIVE, EXPIRED, INVALID, MISMATCH }

    private final Status status;
    private final String message;
    private final License license;

    private LicenseValidationResult(Status status, String message, License license) {
        this.status = status;
        this.message = message;
        this.license = license;
    }

    public static LicenseValidationResult active(License license, String message) {
        return new LicenseValidationResult(Status.ACTIVE, message, license);
    }
    public static LicenseValidationResult expired(String message) {
        return new LicenseValidationResult(Status.EXPIRED, message, null);
    }
    public static LicenseValidationResult invalid(String message) {
        return new LicenseValidationResult(Status.INVALID, message, null);
    }
    public static LicenseValidationResult mismatch(String message) {
        return new LicenseValidationResult(Status.MISMATCH, message, null);
    }

    public boolean isActive() { return status == Status.ACTIVE; }
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public License getLicense() { return license; }
}
