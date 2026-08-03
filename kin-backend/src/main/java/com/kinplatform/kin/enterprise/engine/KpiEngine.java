package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de KPIs (Fase 10).
 *
 * <p>Definirá los KPIs por fase (adquisición, activación, retención, ingresos
 * y margen) como value object {@code KpiSet}, con objetivos derivados del plan
 * de mercado y del plan financiero. Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface KpiEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
