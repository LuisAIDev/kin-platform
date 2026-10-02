package com.kinplatform.licensing.domain;

public enum LicenseModule {
    HCE,
    TRIAJE,
    OCR,
    TELEMEDICINA,
    AGENDA,
    RIPS;

    public static LicenseModule fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Módulo vacío");
        }
        try {
            return LicenseModule.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Módulo desconocido: " + value);
        }
    }
}
