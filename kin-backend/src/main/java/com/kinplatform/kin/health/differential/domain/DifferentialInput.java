package com.kinplatform.kin.health.differential.domain;

import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import java.util.List;

/**
 * Entrada del motor de diagnóstico diferencial (ADR-029).
 *
 * <p>Recibe los síntomas reportados, el {@link TriageConditionResult} base
 * (producido por el {@code TriageEngine}) y el contexto del paciente si existe
 * (p. ej. fumador, embarazo) para ajustar factores de riesgo. Implementa
 * {@link EngineInput} para integrarse con la infraestructura común de motores.</p>
 */
public record DifferentialInput(
        List<String> symptoms, List<TriageConditionResult> triageConditions, PatientContext patientContext)
        implements EngineInput {

    public DifferentialInput {
        symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
        triageConditions = triageConditions == null ? List.of() : List.copyOf(triageConditions);
        patientContext = patientContext == null ? PatientContext.empty() : patientContext;
    }

    public boolean isEmpty() {
        return triageConditions.isEmpty();
    }

    public static DifferentialInput of(List<String> symptoms, List<TriageConditionResult> triageConditions) {
        return new DifferentialInput(symptoms, triageConditions, PatientContext.empty());
    }

    public static DifferentialInput of(
            List<String> symptoms, List<TriageConditionResult> triageConditions, PatientContext patientContext) {
        return new DifferentialInput(symptoms, triageConditions, patientContext);
    }
}
