package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;

/**
 * Contrato del motor de innovación (Fase 10).
 *
 * <p>Producirá el plan de innovación (factores diferenciales, novedad,
 * barreras de imitación y hoja de ruta de I+D) como value object
 * {@code InnovationPlan} y poblará la {@code InnovationSection} del informe de
 * consultoría, reutilizando los analizadores del motor de oportunidades sin
 * duplicar análisis. Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 1 define únicamente el contrato genérico; la entrada (E) y
 * la salida (R) concretas se fijarán en el Milestone 2.</p>
 *
 * @param <E> tipo de entrada (debe extender {@link EngineInput})
 * @param <R> tipo de resultado (debe implementar {@link EngineResult})
 */
public interface InnovationEngine<E extends EngineInput, R extends EngineResult>
        extends DomainEngine<E, R> {
}
