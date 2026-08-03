package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code FinancialPlanEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor
 * financiero necesita (p. ej. modelo de negocio, score y análisis financiero
 * del pipeline) para producir el plan financiero. El Milestone 2A define
 * únicamente el tipo, sin campos ni lógica.</p>
 */
public record FinancialPlanInput() implements EngineInput {
}
