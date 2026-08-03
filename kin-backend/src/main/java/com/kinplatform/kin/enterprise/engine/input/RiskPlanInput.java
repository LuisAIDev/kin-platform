package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code RiskPlanEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor de
 * matriz de riesgos necesita (p. ej. resultado de riesgo del pipeline y plan
 * financiero) para producir la presentación matricial de riesgos. El Milestone
 * 2A define únicamente el tipo, sin campos ni lógica.</p>
 */
public record RiskPlanInput() implements EngineInput {
}
