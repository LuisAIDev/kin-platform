package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code RoadmapEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor de
 * hoja de ruta necesita (p. ej. plan financiero y recomendaciones del pipeline)
 * para producir el roadmap y cronograma. El Milestone 2A define únicamente el
 * tipo, sin campos ni lógica.</p>
 */
public record RoadmapInput() implements EngineInput {
}
