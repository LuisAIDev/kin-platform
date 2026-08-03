package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de hoja de ruta (Fase 10).
 *
 * <p>Producirá el roadmap y cronograma (fases, hitos, camino crítico y
 * entradas de Gantt) como value object {@code Roadmap} y poblará la
 * {@code NextStepsSection} del informe de consultoría, alineando los hitos al
 * plan financiero. Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface RoadmapEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
