package com.kinplatform.kin.export.model;

import java.util.Locale;

/**
 * Formato de salida de la exportación de un proyecto.
 */
public enum ExportFormat {
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    PDF("application/pdf", "pdf"),
    MARKDOWN("text/markdown", "md");

    private final String contentType;
    private final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static ExportFormat parse(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "DOCX" -> DOCX;
            case "PDF" -> PDF;
            case "MARKDOWN", "MD" -> MARKDOWN;
            default -> null;
        };
    }
}
