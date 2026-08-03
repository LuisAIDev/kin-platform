package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Enterprise Score del proyecto empresarial (value object).
 *
 * <p>Contendrá la puntuación empresarial multidimensional (mercado, innovación,
 * viabilidad, finanzas, riesgo, escalabilidad, equipo y sostenibilidad)
 * producida por {@code EnterpriseScoreEngine}, de forma totalmente
 * determinista. El Milestone 2A solo define el contenedor sin lógica; la
 * estructura de dimensiones se detallará en el Milestone 2.</p>
 */
public record EnterpriseScore() {
}
