package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Colector de citas (Fase 5 — Citation Engine): convierte decisiones en entradas
 * de citas, conservando solo las incluidas y deduplicando por {@code (sourceId,
 * url)}. No valida ni decide: solo recolecta y deduplica.
 */
public class CitationCollector {

    public List<CitationEntry> collect(List<KnowledgeFact> facts, List<CitationDecision> decisions) {
        if (facts == null || decisions == null || facts.isEmpty() || decisions.isEmpty()) {
            return List.of();
        }
        var entries = new LinkedHashMap<String, CitationEntry>();
        int size = Math.min(facts.size(), decisions.size());
        for (int i = 0; i < size; i++) {
            KnowledgeFact fact = facts.get(i);
            CitationDecision decision = decisions.get(i);
            if (decision.included() && fact != null) {
                String key = fact.sourceId() + "|" + fact.url();
                if (!entries.containsKey(key)) {
                    entries.put(key, toEntry(fact, decision));
                }
            }
        }
        return List.copyOf(entries.values());
    }

    private static CitationEntry toEntry(KnowledgeFact fact, CitationDecision decision) {
        return new CitationEntry(fact.sourceId(), fact.url(), "", fact.category(),
            fact.publishedAt(), decision.confidence(), "", "", decision.reason());
    }
}
