package com.kinplatform.kin.health.triage.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extractor determinista de síntomas por keywords (ADR-028).
 *
 * <p>Implementación de dominio pura de {@link SymptomExtractor}: normaliza el
 * texto (minúsculas, sin puntuación) y verifica la presencia de cada nombre
 * canónico y alias de síntoma del catálogo. Al detectar un alias devuelve el
 * nombre canónico (p. ej. {@code "cefalea"} → {@code "dolor de cabeza"}). Sin
 * NLP ni IA; sirve de base determinista y de fallback del extractor basado en
 * OpenNLP.</p>
 */
public class KeywordSymptomExtractor implements SymptomExtractor {

    @Override
    public List<String> extract(TriageCatalog catalog, String text) {
        if (catalog == null || catalog.isEmpty() || text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = SymptomNormalizer.normalize(text);
        Set<String> found = new LinkedHashSet<>();
        for (Symptom symptom : catalog.symptoms()) {
            if (normalized.contains(SymptomNormalizer.normalize(symptom.name()))) {
                found.add(symptom.name());
                continue;
            }
            for (String alias : symptom.aliases()) {
                if (normalized.contains(SymptomNormalizer.normalize(alias))) {
                    found.add(symptom.name());
                    break;
                }
            }
        }
        return List.copyOf(found);
    }
}
