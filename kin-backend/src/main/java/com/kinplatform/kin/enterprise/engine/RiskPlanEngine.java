package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de matriz de riesgos (Fase 10).
 *
 * <p>Producirá la presentación matricial de riesgos (probabilidad × impacto,
 * severidad y mitigaciones) como value object {@code RiskMatrix}, transformando
 * el resultado de riesgo del pipeline (sin recalcularlo) y añadiendo el riesgo
 * financiero derivado del plan financiero. Es un {@link DomainEngine}
 * determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface RiskPlanEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
