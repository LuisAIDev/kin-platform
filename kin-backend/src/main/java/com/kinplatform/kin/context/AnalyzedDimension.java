package com.kinplatform.kin.context;

public enum AnalyzedDimension {

    PROJECT_NAME("Nombre del proyecto"),
    SECTOR("Sector / giro del negocio"),
    CITY("Ciudad / ubicaci\u00F3n"),
    PROBLEM("Problema que resuelve"),
    SOLUTION("Soluci\u00F3n propuesta"),
    TARGET_CUSTOMER("Cliente objetivo"),
    VALUE_PROPOSITION("Propuesta de valor"),
    REVENUE_MODEL("Modelo de ingresos"),
    COMPETITION("Competencia"),
    RISKS("Riesgos"),
    RESOURCES("Recursos necesarios"),
    MVP("MVP / validaci\u00F3n temprana"),
    SCALABILITY("Escalabilidad"),
    OBJECTIVES("Objetivos del proyecto");

    private final String displayName;

    AnalyzedDimension(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
