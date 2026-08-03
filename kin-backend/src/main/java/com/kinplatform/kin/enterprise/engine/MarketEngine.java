package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de mercado (Fase 10).
 *
 * <p>Producirá el plan de mercado (mercado direccionable proxy, segmentos,
 * canales, competidores verificados y barreras de entrada) como value object
 * {@code MarketPlan} y poblará la {@code MarketSection} del informe de
 * consultoría, priorizando los hechos verificados del motor de conocimiento.
 * Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface MarketEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
