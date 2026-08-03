package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de plan financiero (Fase 10).
 *
 * <p>Producirá el estudio financiero básico (CAPEX, OPEX, proyección a 3 años,
 * márgenes y punto de equilibrio) como value object {@code FinancialPlan} y
 * poblará la {@code FinancialSection} del informe de consultoría. Es un
 * {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface FinancialPlanEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
