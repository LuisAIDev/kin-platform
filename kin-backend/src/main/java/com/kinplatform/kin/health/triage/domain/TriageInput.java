package com.kinplatform.kin.health.triage.domain;

import com.kinplatform.common.engine.EngineInput;
import java.util.List;

/**
 * Entrada del motor de triaje (ADR-028).
 *
 * <p>Lista de síntomas reportados por el paciente (nombres canónicos del
 * catálogo o identificadores). Implementa {@link EngineInput} para integrarse
 * con la infraestructura común de motores.</p>
 */
public record TriageInput(List<String> symptoms) implements EngineInput {

    public TriageInput {
        symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
    }

    public static TriageInput of(List<String> symptoms) {
        return new TriageInput(symptoms);
    }

    public boolean isEmpty() {
        return symptoms.isEmpty();
    }
}

