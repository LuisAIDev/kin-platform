package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.InnovationInput;
import com.kinplatform.kin.enterprise.engine.result.InnovationResult;

/**
 * Contrato del motor de innovación (Fase 10).
 *
 * <p>Producirá el plan de innovación (factores diferenciales, novedad,
 * barreras de imitación y hoja de ruta de I+D) como value object
 * {@code InnovationPlan} (vía {@link InnovationResult}) a partir de la entrada
 * tipada {@link InnovationInput}, y poblará la {@code InnovationSection} del
 * informe de consultoría, reutilizando los analizadores del motor de
 * oportunidades sin duplicar análisis. Es un {@link DomainEngine}
 * determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface InnovationEngine
        extends DomainEngine<InnovationInput, InnovationResult> {
}
