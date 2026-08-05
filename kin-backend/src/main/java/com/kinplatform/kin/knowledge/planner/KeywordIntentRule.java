package com.kinplatform.kin.knowledge.planner;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Regla de intención por palabras clave (especificación Fase 3): coincide si el
 * texto normalizado contiene alguna de sus palabras clave. Determinista, sin IA.
 *
 * <p>Para frases de una sola palabra usa coincidencia por tokens (evita que
 * "sas" coincida con "casa"); para frases de varias palabras usa subcadena.
 * La normalización elimina acentos y convierte a minúsculas.</p>
 */
public final class KeywordIntentRule implements IntentRule {

    private final String name;
    private final IntentFacet facet;
    private final boolean stable;
    private final Set<String> keywords;

    public KeywordIntentRule(String name, IntentFacet facet, Set<String> keywords, boolean stable) {
        this.name = name == null ? "" : name;
        this.facet = facet;
        this.stable = stable;
        this.keywords = keywords == null ? Set.of() : Set.copyOf(keywords);
    }

    public static KeywordIntentRule of(IntentFacet facet, String... keywords) {
        return new KeywordIntentRule(facet.name(), facet, Set.of(keywords), false);
    }

    public static KeywordIntentRule stable(String name, String... keywords) {
        return new KeywordIntentRule(name, null, Set.of(keywords), true);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public IntentFacet facet() {
        return facet;
    }

    @Override
    public boolean stable() {
        return stable;
    }

    @Override
    public boolean matches(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        for (var keyword : keywords) {
            if (containsKeyword(normalizedText, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normaliza texto de dominio: minúsculas y sin acentos (p. ej. "panadería"
     * → "panaderia"). Permite que las palabras clave se escriban sin acentos.
     */
    static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String lower = input.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }

    private static boolean containsKeyword(String normalizedText, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        if (keyword.contains(" ")) {
            return normalizedText.contains(keyword);
        }
        return tokens(normalizedText).contains(keyword);
    }

    private static Set<String> tokens(String normalizedText) {
        var tokens = new LinkedHashSet<String>();
        for (var part : normalizedText.split("[^a-z0-9]+")) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
