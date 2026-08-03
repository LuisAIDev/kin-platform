package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code InnovationEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor de
 * innovación necesita (p. ej. oportunidades y análisis de innovación del
 * pipeline) para producir el plan de innovación. El Milestone 2A define
 * únicamente el tipo, sin campos ni lógica.</p>
 */
public record InnovationInput() implements EngineInput {
}
