package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Estrategia de coincidencia difusa (fuzzy match):
 * - Para claims: usa similitud Jaro-Winkler normalizada (0.0 - 1.0)
 * - Para valores numéricos: tolerancia porcentual configurable
 * - Umbral configurable (default 0.85)
 */
public class FuzzyMatchStrategy implements DeduplicationStrategy {

    public static final String STRATEGY_NAME = "FUZZY_MATCH";

    final double threshold;

    public FuzzyMatchStrategy() {
        this(0.85);
    }

    public FuzzyMatchStrategy(double threshold) {
        this.threshold = Math.max(0.0, Math.min(1.0, threshold));
    }

    @Override
    public String strategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public int priority() {
        return 20; // Segunda prioridad
    }

    @Override
    public boolean areDuplicates(KnowledgeFact a, KnowledgeFact b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        if (!a.sourceId().equals(b.sourceId())) {
            return false;
        }
        if (!a.category().equals(b.category())) {
            return false;
        }

        double similarity = computeSimilarity(a.claim(), b.claim());
        return similarity >= threshold;
    }

    /**
     * Calcula similitud entre dos claims usando Jaro-Winkler normalizado.
     * Normaliza: trim, lowercase, remover acentos.
     */
    double computeSimilarity(String claimA, String claimB) {
        if (claimA == null || claimB == null) {
            return 0.0;
        }
        String normA = normalize(claimA);
        String normB = normalize(claimB);
        if (normA.equals(normB)) {
            return 1.0;
        }
        return jaroWinklerSimilarity(normA, normB);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        // Normalizar: trim, lowercase, remover acentos
        String noAccents = Normalizer.normalize(text.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noAccents.replaceAll("\\s+", " ").trim();
    }

    /**
     * Implementación de Jaro-Winkler similarity (0.0 - 1.0).
     * Basada en la implementación estándar.
     */
    private double jaroWinklerSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        if (matchDistance < 0) {
            matchDistance = 0;
        }

        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        int transpositions = 0;

        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (!s2Matches[j] && s1.charAt(i) == s2.charAt(j)) {
                    s1Matches[i] = true;
                    s2Matches[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (s1Matches[i]) {
                while (!s2Matches[k]) {
                    k++;
                }
                if (s1.charAt(i) != s2.charAt(k)) {
                    transpositions++;
                }
                k++;
            }
        }

        double jaro = (matches / (double) len1
                + matches / (double) len2
                + (matches - transpositions / 2.0) / matches) / 3.0;

        // Winkler adjustment: boost prefix matches up to 4 chars
        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(s1.length(), s2.length()));
        for (int i = 0; i < maxPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }

        return jaro + (0.1 * prefixLength * (1 - jaro));
    }
}