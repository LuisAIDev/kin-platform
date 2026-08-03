package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.RoadmapInput;
import com.kinplatform.kin.enterprise.engine.result.RoadmapResult;

/**
 * Contrato del motor de hoja de ruta (Fase 10).
 *
 * <p>Producirá el roadmap y cronograma (fases, hitos, camino crítico y
 * entradas de Gantt) como value object {@code Roadmap} (vía
 * {@link RoadmapResult}) a partir de la entrada tipada {@link RoadmapInput}, y
 * poblará la {@code NextStepsSection} del informe de consultoría, alineando los
 * hitos al plan financiero. Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface RoadmapEngine
        extends DomainEngine<RoadmapInput, RoadmapResult> {
}
