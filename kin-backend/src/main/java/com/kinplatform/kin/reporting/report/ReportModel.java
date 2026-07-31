package com.kinplatform.kin.reporting.report;

/**
 * Configuración del {@link ReportEngine}. Value Object inmutable e inyectable:
 * versión del reporte, versión de arquitectura y límite de próximos pasos.
 */
public class ReportModel {

    private final String version;
    private final String architectureVersion;
    private final String description;
    private final int nextStepsLimit;

    public ReportModel(String version, String architectureVersion, String description, int nextStepsLimit) {
        this.version = version == null ? "" : version;
        this.architectureVersion = architectureVersion == null ? "" : architectureVersion;
        this.description = description == null ? "" : description;
        this.nextStepsLimit = Math.max(0, nextStepsLimit);
    }

    public String version() {
        return version;
    }

    public String architectureVersion() {
        return architectureVersion;
    }

    public String description() {
        return description;
    }

    public int nextStepsLimit() {
        return nextStepsLimit;
    }

    public static ReportModel defaultModel() {
        return new ReportModel("v1", "2.0.0-alpha.1",
            "Modelo del reporte de consultoría KIN: proyección de resultados ya calculados, no recálculo",
            5);
    }
}
