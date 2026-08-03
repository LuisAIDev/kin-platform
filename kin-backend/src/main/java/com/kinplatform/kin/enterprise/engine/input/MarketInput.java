package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code MarketEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor de
 * mercado necesita (p. ej. hechos verificados del motor de conocimiento y el
 * análisis de mercado del pipeline) para producir el plan de mercado. El
 * Milestone 2A define únicamente el tipo, sin campos ni lógica.</p>
 */
public record MarketInput() implements EngineInput {
}
