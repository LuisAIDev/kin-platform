package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Catálogo de documentos que conforman el proyecto empresarial de la Fase 10.
 *
 * <p>Define los tipos de documento que KIN puede generar para un proyecto
 * empresarial. Cada valor se corresponde con un {@code DocumentArtifact} del
 * aggregate {@code EnterpriseProject}. El documento {@code DOFA} no posee un
 * value object propio: se compone a partir de los demás modelos
 * (ensamblador {@code DofaAssembler}).</p>
 */
public enum DocumentType {
    EXECUTIVE_REPORT,
    LEAN_CANVAS,
    DOFA,
    FINANCIAL_PLAN,
    MARKET_PLAN,
    ROADMAP,
    RISK_MATRIX,
    KPI,
    INNOVATION_PLAN
}
