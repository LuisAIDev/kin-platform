package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.FinancialPlanInput;
import com.kinplatform.kin.enterprise.engine.result.FinancialPlanResult;

/**
 * Contrato del motor de plan financiero (Fase 10).
 *
 * <p>Producirá el estudio financiero básico (CAPEX, OPEX, proyección a 3 años,
 * márgenes y punto de equilibrio) como value object {@code FinancialPlan} (vía
 * {@link FinancialPlanResult}) a partir de la entrada tipada
 * {@link FinancialPlanInput}, y poblará la {@code FinancialSection} del informe
 * de consultoría. Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface FinancialPlanEngine
        extends DomainEngine<FinancialPlanInput, FinancialPlanResult> {
}
