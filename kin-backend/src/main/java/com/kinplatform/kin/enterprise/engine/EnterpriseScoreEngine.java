package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de puntuación empresarial (Fase 10).
 *
 * <p>Calculará el Enterprise Score multidimensional (mercado, innovación,
 * viabilidad, finanzas, riesgo, escalabilidad, equipo y sostenibilidad) a
 * partir de los value objects del proyecto empresarial, de forma totalmente
 * determinista (Java decide; el LLM solo comunica). Es un {@link DomainEngine}
 * determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface EnterpriseScoreEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
