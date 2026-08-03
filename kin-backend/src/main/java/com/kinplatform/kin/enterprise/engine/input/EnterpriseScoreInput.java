package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code EnterpriseScoreEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los value objects del
 * proyecto empresarial que el motor de puntuación consume para calcular el
 * Enterprise Score multidimensional. El Milestone 2A define únicamente el
 * tipo, sin campos ni lógica.</p>
 */
public record EnterpriseScoreInput() implements EngineInput {
}
