package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Matriz de riesgos del proyecto empresarial (value object).
 *
 * <p>Contendrá la presentación matricial de los riesgos (probabilidad ×
 * impacto, severidad y mitigaciones) derivada del resultado de riesgo del
 * pipeline, producida por {@code RiskPlanEngine}. El Milestone 1 solo define
 * el contenedor sin lógica; la estructura de campos se detallará en el
 * Milestone 2.</p>
 */
public record RiskMatrix() {
}
