package com.kinplatform.kin.health.triage.domain;

import java.util.List;
import java.util.Locale;

/**
 * Normalizador de términos de salud (ADR-028, fase profesional).
 *
 * <p>Utilidad de dominio pura (sin infraestructura) que normaliza términos
 * libres (sinónimos, acentos, plurales) al nombre canónico de un síntoma del
 * catálogo. Java decide la normalización; el NLP únicamente ayuda a extraer
 * candidatos del texto libre.</p>
 */
public final class SymptomNormalizer {

    private SymptomNormalizer() {}

    /**
     * Normaliza un término libre a minúsculas sin signos de puntuación,
     * preservando las tildes (la coincidencia de alias incluye las tildes).
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9áéíóúüñ\\s]", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    /**
     * Resuelve un término libre al nombre canónico de un síntoma del catálogo,
     * verificando nombre canónico y aliases. {@code null} si no hay coincidencia.
     */
    public static String resolve(TriageCatalog catalog, String term) {
        if (catalog == null || catalog.isEmpty() || term == null || term.isBlank()) {
            return null;
        }
        String normalized = normalize(term);
        if (normalized.isBlank()) {
            return null;
        }
        for (Symptom symptom : catalog.symptoms()) {
            if (symptom.name().equalsIgnoreCase(normalized)) {
                return symptom.name();
            }
            for (String alias : symptom.aliases()) {
                if (normalize(alias).equals(normalized)) {
                    return symptom.name();
                }
            }
        }
        return null;
    }

    /**
     * Devuelve todos los términos reconocibles (nombre canónico + aliases) de un
     * síntoma, normalizados. Útil para construir diccionarios de entidades (NLP).
     */
    public static List<String> allTerms(Symptom symptom) {
        if (symptom == null) {
            return List.of();
        }
        if (symptom.aliases() == null || symptom.aliases().isEmpty()) {
            return List.of(normalize(symptom.name()));
        }
        var out = new java.util.ArrayList<String>();
        out.add(normalize(symptom.name()));
        for (String alias : symptom.aliases()) {
            String norm = normalize(alias);
            if (!norm.isBlank() && !out.contains(norm)) {
                out.add(norm);
            }
        }
        return List.copyOf(out);
    }
}
