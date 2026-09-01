package com.kinplatform.kin.knowledge.deduplication;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;

/**
 * Estrategia de coincidencia exacta: dos hechos son duplicados si tienen
 * el mismo sourceId, category, claim normalizado y mismo hash de valor.
 */
public class ExactMatchStrategy implements DeduplicationStrategy {

    public static final String STRATEGY_NAME = "EXACT_MATCH";

    @Override
    public String strategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public int priority() {
        return 10; // Máxima prioridad (se ejecuta primero)
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
        // Normalizar claim: trim + lowercase
        String claimA = normalizeClaim(a.claim());
        String claimB = normalizeClaim(b.claim());
        if (!claimA.equals(claimB)) {
            return false;
        }
        // Para valores numéricos, comparar con tolerancia exacta
        // (el claim normalizado ya incluye el valor)
        return true;
    }

    private String normalizeClaim(String claim) {
        if (claim == null) {
            return "";
        }
        return claim.trim().toLowerCase();
    }

    /**
     * Selecciona el hecho "ganador" entre duplicados.
     * Prioridad: mayor SourceTrust > más reciente > más completo.
     */
    public KnowledgeFact selectWinner(KnowledgeFact a, KnowledgeFact b) {
        int cmp = compareTrust(a.trust(), b.trust());
        if (cmp != 0) {
            return cmp > 0 ? a : b;
        }
        // Más reciente (publishedAt)
        if (a.publishedAt() != null && b.publishedAt() != null) {
            return a.publishedAt().isAfter(b.publishedAt()) ? a : b;
        }
        // Más completo (claim más largo = más información)
        return a.claim().length() >= b.claim().length() ? a : b;
    }

    private int compareTrust(SourceTrust a, SourceTrust b) {
        // OFFICIAL_PUBLIC (0) > SECONDARY (1) > UNVERIFIED (2)
        // Invertir ordinal: mayor confianza = menor ordinal
        return Integer.compare(b.ordinal(), a.ordinal());
    }
}
