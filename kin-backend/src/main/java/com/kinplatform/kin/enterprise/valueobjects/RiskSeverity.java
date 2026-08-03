package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Severidad de un riesgo de la matriz de riesgos (value object).
 *
 * <p>Clasifica el nivel de severidad derivado de la probabilidad × impacto:
 * {@code LOW}, {@code MEDIUM}, {@code HIGH} o {@code CRITICAL}. Usado por
 * {@link RiskMatrix.Risk}.</p>
 */
public enum RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
