package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.domain.Symptom;
import java.util.UUID;

/**
 * Síntoma disponible del catálogo (endpoint {@code GET /health/triage/symptoms}).
 */
public record SymptomResponse(UUID id, String name, String description, String icdCode) {

    public static SymptomResponse from(Symptom symptom) {
        return new SymptomResponse(symptom.id(), symptom.name(), symptom.description(), symptom.icdCode());
    }
}
