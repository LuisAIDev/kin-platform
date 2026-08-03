package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Plan financiero del proyecto empresarial (value object).
 *
 * <p>Contendrá el estudio financiero básico (CAPEX, OPEX, proyección de
 * ingresos a 3 años, márgenes, punto de equilibrio y escenarios), producido
 * por {@code FinancialPlanEngine}. El Milestone 1 solo define el contenedor
 * sin lógica; la estructura de campos se detallará en el Milestone 2.</p>
 */
public record FinancialPlan() {
}
