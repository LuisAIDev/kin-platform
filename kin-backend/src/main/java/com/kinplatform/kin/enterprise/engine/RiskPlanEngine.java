package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.RiskPlanInput;
import com.kinplatform.kin.enterprise.engine.result.RiskPlanResult;

/**
 * Contrato del motor de matriz de riesgos (Fase 10).
 *
 * <p>Producirá la presentación matricial de riesgos (probabilidad × impacto,
 * severidad y mitigaciones) como value object {@code RiskMatrix} (vía
 * {@link RiskPlanResult}) a partir de la entrada tipada
 * {@link RiskPlanInput}, transformando el resultado de riesgo del pipeline (sin
 * recalcularlo) y añadiendo el riesgo financiero derivado del plan financiero.
 * Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface RiskPlanEngine
        extends DomainEngine<RiskPlanInput, RiskPlanResult> {
}
