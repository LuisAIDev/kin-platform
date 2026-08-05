package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;

import java.util.Locale;

/**
 * Citation Engine (Fase 5): fachada determinista e inmutable que transforma un
 * {@link KnowledgeResult} en un {@link CitationBundle} mediante la cadena
 * decisión → colección → formato.
 *
 * <p>Nunca consulta proveedores, nunca accede a Internet y nunca modifica los
 * {@link KnowledgeFact}. POJO puro de dominio: el único contrato de salida que
 * consumirá la construcción del prompt es el {@link CitationBundle}.</p>
 */
public class CitationEngine {

    private final CitationCollector collector;
    private final CitationFormatterRegistry formatters;

    public CitationEngine() {
        this(new CitationCollector(), CitationFormatterRegistry.defaults());
    }

    public CitationEngine(CitationCollector collector, CitationFormatterRegistry formatters) {
        this.collector = collector == null ? new CitationCollector() : collector;
        this.formatters = formatters == null ? CitationFormatterRegistry.defaults() : formatters;
    }

    /**
     * Produce el resultado de citación para un {@link KnowledgeResult} bajo una
     * política y un estilo dados. Determinista: misma entrada → misma salida.
     */
    public CitationResult produce(KnowledgeResult result, CitationPolicy policy, CitationStyle style) {
        var safePolicy = policy == null ? new VerifiedCitationPolicy() : policy;
        var safeStyle = style == null ? CitationStyle.INLINE : style;
        if (safeStyle == CitationStyle.DISABLED) {
            return CitationResult.empty(safeStyle, "Citación deshabilitada por estilo.");
        }
        if (result == null || result.facts().isEmpty()) {
            return CitationResult.empty(safeStyle, "Sin hechos verificados para citar.");
        }
        var facts = result.facts();
        var decisions = new java.util.ArrayList<CitationDecision>();
        for (var fact : facts) {
            decisions.add(safePolicy.decide(fact));
        }
        var entries = collector.collect(facts, decisions);
        var references = new java.util.ArrayList<String>();
        if (safeStyle != CitationStyle.HIDDEN) {
            for (int i = 0; i < entries.size(); i++) {
                references.add(formatters.formatterFor(safeStyle).format(entries.get(i), i + 1));
            }
        }
        var metadata = CitationMetadata.of(entries);
        double score = computeScore(entries);
        String explanation = buildExplanation(entries.size(), facts.size(), score);
        var bundle = new CitationBundle(safeStyle, entries, java.util.List.copyOf(references),
            metadata, score, explanation);
        return new CitationResult(bundle, java.util.List.copyOf(decisions));
    }

    private static double computeScore(java.util.List<CitationEntry> entries) {
        if (entries.isEmpty()) {
            return 0.0;
        }
        return entries.stream().mapToDouble(CitationEntry::confidence).average().orElse(0.0);
    }

    private static String buildExplanation(int cited, int total, double score) {
        String percent = String.format(Locale.ROOT, "%.0f", score * 100);
        return "Citas verificadas: " + cited + " de " + total + ". Confianza media: " + percent + "%.";
    }
}
