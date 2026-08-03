package com.kinplatform.kin.enterprise.engine.input;

import com.kinplatform.kin.engine.EngineInput;

/**
 * Entrada tipada del {@code KpiEngine} (Fase 10, Milestone 2A).
 *
 * <p>Contrato de entrada: en el Milestone 2 expondrá los datos que el motor de
 * KPIs necesita (p. ej. plan de mercado y plan financiero) para definir los
 * KPIs por fase. El Milestone 2A define únicamente el tipo, sin campos ni
 * lógica.</p>
 */
public record KpiInput() implements EngineInput {
}
