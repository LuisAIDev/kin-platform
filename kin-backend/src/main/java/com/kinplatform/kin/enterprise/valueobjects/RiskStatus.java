package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Estado de gestión de un riesgo de la matriz (value object).
 *
 * <p>Indica el ciclo de gestión del riesgo: {@code IDENTIFIED} (identificado,
 * sin acción aún), {@code MITIGATING} (en mitigación), {@code MITIGATED}
 * (mitigado) o {@code ACCEPTED} (aceptado). Usado por
 * {@link RiskMatrix.Risk}.</p>
 */
public enum RiskStatus {
    IDENTIFIED,
    MITIGATING,
    MITIGATED,
    ACCEPTED
}
