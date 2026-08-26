package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.kin.health.triage.domain.KeywordSymptomExtractor;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomExtractor;
import com.kinplatform.kin.health.triage.domain.SymptomNormalizer;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extractor de síntomas con NER mejorado para textos clínicos en español
 * (ADR-028, fase de consolidación).
 *
 * <p>Mejora el {@link OpenNLPSymptomExtractor} con:
 * <ul>
 *   <li><strong>Contextos de negación</strong>: maneja prefijos como
 *       {@code "no tengo"}, {@code "sin"}, {@code "no siento"} para descartar
 *       síntomas.</li>
 *   <li><strong>Parafraseo "me duele X"</strong>: detecta expresiones como
 *       {@code "me duele la cabeza"} y las mapea al síntoma canónico
 *       {@code "dolor de cabeza"} usando un índice de partes del cuerpo
 *       derivado del catálogo.</li>
 *   <li><strong>Mediciones e intensidad</strong>: extrae el síntoma subyacente
 *       de {@code "fiebre de 38°"} o {@code "tos muy seca"}.</li>
 *   <li><strong>Tokenización con n-gramas de 1..3</strong> para términos
 *       compuestos del catálogo.</li>
 *   <li><strong>Fallback determinista</strong> a {@link KeywordSymptomExtractor}.</li>
 * </ul>
 *
 * <p>El NLP solo amplía los candidatos; la normalización final y el cálculo de
 * probabilidades siguen siendo 100 % Java/deterministas.</p>
 */
public class OpenNLPSymptomExtractorV2 implements SymptomExtractor {

    private static final Logger log = LoggerFactory.getLogger(OpenNLPSymptomExtractorV2.class);

    private static final List<String> NEGATIONS =
            List.of("no", "sin", "sin tener", "no tengo", "no siento", "no padezco");

    private final KeywordSymptomExtractor fallback = new KeywordSymptomExtractor();
    private final WhitespaceTokenizer tokenizer = WhitespaceTokenizer.INSTANCE;

    @Override
    public List<String> extract(TriageCatalog catalog, String text) {
        if (catalog == null || catalog.isEmpty() || text == null || text.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Boolean> candidates = scan(catalog, text);
            if (!candidates.isEmpty()) {
                return candidates.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .toList();
            }
        } catch (RuntimeException ex) {
            log.debug("OpenNLPSymptomExtractorV2: fallback a keywords ({})", ex.getMessage());
        }
        return fallback.extract(catalog, text);
    }

    /**
     * Escanea el texto y devuelve nombre canónico → presente (no negado).
     */
    private Map<String, Boolean> scan(TriageCatalog catalog, String text) {
        String normalized = SymptomNormalizer.normalize(text);
        String[] tokens = tokenizer.tokenize(normalized);
        Map<String, Boolean> found = new LinkedHashMap<>();
        for (Symptom symptom : catalog.symptoms()) {
            boolean matched = false;
            boolean negated = false;
            for (String term : SymptomNormalizer.allTerms(symptom)) {
                if (term.isBlank()) {
                    continue;
                }
                int idx = indexOfTerm(tokens, term);
                if (idx >= 0) {
                    matched = true;
                    if (isNegated(tokens, idx)) {
                        negated = true;
                        break;
                    }
                }
            }
            if (matched) {
                found.put(symptom.name(), !negated);
            }
        }
        // Parafraseo "me duele X" → "dolor de X"
        if (normalized.contains("duele") || normalized.contains("me duele")) {
            applyPainParaphrase(catalog, tokens, normalized, found);
        }
        return found;
    }

    /**
     * Detecta expresiones del tipo "me duele la cabeza" y las mapea al síntoma
     * canónico "dolor de cabeza" derivado del catálogo.
     */
    private void applyPainParaphrase(
            TriageCatalog catalog, String[] tokens, String normalized, Map<String, Boolean> found) {
        if (!normalized.contains("duele")) {
            return;
        }
        Map<String, String> bodyPartToSymptom = bodyPartIndex(catalog);
        for (var entry : bodyPartToSymptom.entrySet()) {
            if (found.containsKey(entry.getValue())) {
                continue;
            }
            String part = entry.getKey();
            // "me duele la cabeza", "duele cabeza", "me duele mucho la cabeza" ...
            if (normalized.contains(part) && !containsNegationBefore(normalized, part)) {
                found.put(entry.getValue(), true);
            }
        }
    }

    /**
     * Índice parte del cuerpo → síntoma canónico "dolor de <parte>".
     */
    private Map<String, String> bodyPartIndex(TriageCatalog catalog) {
        Map<String, String> index = new LinkedHashMap<>();
        for (Symptom symptom : catalog.symptoms()) {
            String name = symptom.name();
            if (name.startsWith("dolor de ")) {
                String part = SymptomNormalizer.normalize(name)
                        .replace("dolor de ", "")
                        .trim();
                if (!part.isBlank()) {
                    index.putIfAbsent(part, symptom.name());
                }
            }
        }
        return index;
    }

    private boolean containsNegationBefore(String normalized, String part) {
        int partIdx = normalized.indexOf(part);
        if (partIdx < 0) {
            return false;
        }
        int start = Math.max(0, partIdx - 15);
        String window = normalized.substring(start, partIdx);
        for (String negation : NEGATIONS) {
            if (window.contains(negation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca el término (1..3 tokens) dentro del array tokenizado.
     */
    private int indexOfTerm(String[] tokens, String term) {
        String[] parts = term.split(" ");
        if (parts.length == 1) {
            return indexOf(tokens, parts[0]);
        }
        for (int i = 0; i <= tokens.length - parts.length; i++) {
            boolean match = true;
            for (int j = 0; j < parts.length; j++) {
                if (!tokens[i + j].equals(parts[j])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private int indexOf(String[] tokens, String token) {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(token)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determina si el síntoma está negado: busca una negación en los 3 tokens
     * previos al término.
     */
    private boolean isNegated(String[] tokens, int idx) {
        int start = Math.max(0, idx - 3);
        for (int i = start; i < idx; i++) {
            if (NEGATIONS.contains(tokens[i])) {
                return true;
            }
        }
        return false;
    }
}
