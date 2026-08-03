package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de modelo de negocio (Fase 10).
 *
 * <p>Propondrá el modelo de negocio y la propuesta de valor produciendo el
 * value object {@code LeanCanvas} a partir del contexto del proyecto y de los
 * resultados del pipeline. Es un {@link DomainEngine} determinista: Java
 * decide, DeepSeek únicamente comunica.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2 cuando se definan los
 * tipos de entrada del proyecto empresarial.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface BusinessModelEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
