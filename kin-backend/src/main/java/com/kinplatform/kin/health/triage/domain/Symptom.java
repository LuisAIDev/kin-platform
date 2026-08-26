package com.kinplatform.kin.health.triage.domain;

import java.util.List;
import java.util.UUID;

/**
 * Síntoma del catálogo de triaje (ADR-028).
 *
 * <p>Entidad de dominio inmutable: identifica un síntoma por su id, nombre
 * canónico, descripción, código CIE-10 opcional y una lista de {@code aliases}
 * (términos alternativos para la normalización NLP del {@code SymptomExtractor},
 * p. ej. {@code "cefalea"} como alias de {@code "dolor de cabeza"}).</p>
 */
public record Symptom(UUID id, String name, String description, String icdCode, List<String> aliases) {

    public Symptom {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name no puede ser null o vacío");
        }
        description = description == null ? "" : description;
        icdCode = icdCode == null ? "" : icdCode;
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public static Symptom of(UUID id, String name, String description, String icdCode) {
        return new Symptom(id, name, description, icdCode, List.of());
    }

    public static Symptom of(UUID id, String name, String description, String icdCode, List<String> aliases) {
        return new Symptom(id, name, description, icdCode, aliases);
    }
}
