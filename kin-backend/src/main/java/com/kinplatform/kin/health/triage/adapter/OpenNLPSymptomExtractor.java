package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.kin.health.triage.domain.KeywordSymptomExtractor;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomExtractor;
import com.kinplatform.kin.health.triage.domain.SymptomNormalizer;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extractor de síntomas basado en OpenNLP (ADR-028, fase profesional).
 *
 * <p>Infraestructura: usa el {@link WhitespaceTokenizer} y el
 * {@link DictionaryNameFinder} de OpenNLP (no requieren descarga de modelos)
 * con un diccionario construido a partir del catálogo (nombres canónicos +
 * aliases). El NLP solo amplía los candidatos detectados; la normalización
 * final y el fallback determinista los decide Java con
 * {@link KeywordSymptomExtractor}. Si OpenNLP falla (catalog vacío, excepción),
 * se degrada con gracia al extractor por keywords.</p>
 */
public class OpenNLPSymptomExtractor implements SymptomExtractor {

    private static final Logger log = LoggerFactory.getLogger(OpenNLPSymptomExtractor.class);

    private final KeywordSymptomExtractor fallback = new KeywordSymptomExtractor();
    private final WhitespaceTokenizer tokenizer = WhitespaceTokenizer.INSTANCE;

    @Override
    public List<String> extract(TriageCatalog catalog, String text) {
        if (catalog == null || catalog.isEmpty() || text == null || text.isBlank()) {
            return List.of();
        }
        try {
            Dictionary dictionary = toDictionary(catalog);
            DictionaryNameFinder finder = new DictionaryNameFinder(dictionary);
            String normalized = SymptomNormalizer.normalize(text);
            String[] tokens = tokenizer.tokenize(normalized);
            Span[] spans = finder.find(tokens);
            Set<String> found = new LinkedHashSet<>();
            for (Span span : spans) {
                String term = String.join(" ", java.util.Arrays.copyOfRange(tokens, span.getStart(), span.getEnd()));
                String canonical = resolveCanonical(catalog, term);
                if (canonical != null) {
                    found.add(canonical);
                }
            }
            if (!found.isEmpty()) {
                return List.copyOf(found);
            }
        } catch (RuntimeException ex) {
            log.debug("OpenNLPSymptomExtractor: fallback a keywords ({})", ex.getMessage());
        }
        return fallback.extract(catalog, text);
    }

    /**
     * Construye el diccionario OpenNLP con los términos reconocibles de cada
     * síntoma (nombre canónico + aliases), separando términos compuestos.
     */
    private Dictionary toDictionary(TriageCatalog catalog) {
        Dictionary dictionary = new Dictionary();
        for (Symptom symptom : catalog.symptoms()) {
            for (String term : SymptomNormalizer.allTerms(symptom)) {
                if (!term.isBlank()) {
                    dictionary.put(new StringList(term.split(" ")));
                }
            }
        }
        return dictionary;
    }

    private String resolveCanonical(TriageCatalog catalog, String term) {
        String canonical = SymptomNormalizer.resolve(catalog, term);
        if (canonical != null) {
            return canonical;
        }
        // El DictionaryNameFinder puede devolver un token suelto de un término
        // compuesto (p. ej. "cabeza" de "dolor de cabeza"): se re-verifica con el
        // extractor determinista de keywords, que exige la frase completa.
        return null;
    }
}
