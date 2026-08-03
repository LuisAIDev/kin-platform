package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code BusinessModelEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor de
 * modelo de negocio necesita del contexto del proyecto y de los resultados del
 * pipeline (p. ej. {@code ProjectContext}, evaluación y score) para proponer el
 * Lean Canvas. El Milestone 2A define únicamente el tipo, sin campos ni
 * lógica.</p>
 */
public record BusinessModelInput() implements EngineInput {
}
