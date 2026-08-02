package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ranquador determinista de evidencia (ADR-016, Etapa E3).
 *
 * <p>POJO puro, sin Spring y sin IA: puntúa cada {@link KnowledgeFact} contra
 * cada {@link EvidenceCategory} combinando la cobertura semántica de términos
 * del hecho, la confianza de la fuente ({@link SourceTrust}) y la frescura
 * (edad del dato respecto a un tiempo de referencia). El mismo input produce
 * siempre el mismo ranking (reproducibilidad total).</p>
 */
public final class FactRanker {

    public static final int DEFAULT_MAX_EVIDENCE_PER_CATEGORY = 5;
    public static final double DEFAULT_KEYWORD_WEIGHT = 0.5;
    public static final double DEFAULT_TRUST_WEIGHT = 0.3;
    public static final double DEFAULT_FRESHNESS_WEIGHT = 0.2;
    static final double FULL_KEYWORD_COVERAGE = 3.0;
    static final double FRESH_DAYS = 30.0;
    static final double RECENT_DAYS = 90.0;
    static final double ACCEPTABLE_DAYS = 180.0;
    static final double OLD_DAYS = 365.0;

    private static final Map<EvidenceCategory, Set<String>> KEYWORDS = Map.of(
        EvidenceCategory.MARKET,
            Set.of("mercado", "market", "demanda", "cliente", "segmento", "consumidor", "sector"),
        EvidenceCategory.INNOVATION,
            Set.of("innovacion", "innovación", "patente", "tecnologia", "tecnología", "disruptivo", "disrupción", "diferencial"),
        EvidenceCategory.FINANCIAL,
            Set.of("financier", "ingreso", "revenue", "costo", "cost", "margen", "rentabilidad", "inversion", "inversión", "retorno"),
        EvidenceCategory.COMPETITIVE,
            Set.of("competencia", "competidor", "competitor", "rival", "barrera", "entry")
    );

    private static final Map<SourceTrust, Double> TRUST_FACTORS = Map.of(
        SourceTrust.OFFICIAL_PUBLIC, 1.0,
        SourceTrust.SECONDARY, 0.6,
        SourceTrust.UNVERIFIED, 0.3
    );

    private final double keywordWeight;
    private final double trustWeight;
    private final double freshnessWeight;
    private final OffsetDateTime referenceTime;

    public FactRanker() {
        this(DEFAULT_KEYWORD_WEIGHT, DEFAULT_TRUST_WEIGHT, DEFAULT_FRESHNESS_WEIGHT,
            OffsetDateTime.now());
    }

    public FactRanker(double keywordWeight, double trustWeight) {
        this(keywordWeight, trustWeight, DEFAULT_FRESHNESS_WEIGHT, OffsetDateTime.now());
    }

    public FactRanker(double keywordWeight, double trustWeight, double freshnessWeight) {
        this(keywordWeight, trustWeight, freshnessWeight, OffsetDateTime.now());
    }

    public FactRanker(double keywordWeight, double trustWeight, double freshnessWeight,
                      OffsetDateTime referenceTime) {
        this.keywordWeight = clamp(keywordWeight);
        this.trustWeight = clamp(trustWeight);
        this.freshnessWeight = clamp(freshnessWeight);
        this.referenceTime = referenceTime == null ? OffsetDateTime.now() : referenceTime;
    }

    public List<EvidenceRank> rank(EnrichmentInput input) {
        return rank(input, DEFAULT_MAX_EVIDENCE_PER_CATEGORY);
    }

    public List<EvidenceRank> rank(EnrichmentInput input, int maxEvidencePerCategory) {
        if (input == null || input.knowledge() == null) {
            return List.of();
        }
        int max = Math.max(0, maxEvidencePerCategory);
        List<EvidenceRank> ranks = new ArrayList<>();
        for (EvidenceCategory category : input.categories()) {
            Map<UUID, KnowledgeEvidence> byFactId = new HashMap<>();
            for (KnowledgeFact fact : input.knowledge().facts()) {
                EvidenceScore score = score(fact, category);
                if (score.value() > 0.0 && score.isRelevant(input.minScore())) {
                    byFactId.putIfAbsent(fact.id(), new KnowledgeEvidence(fact, score));
                }
            }
            List<KnowledgeEvidence> top = byFactId.values().stream()
                .sorted(Comparator.comparingDouble(KnowledgeEvidence::scoreValue).reversed())
                .limit(max)
                .toList();
            if (!top.isEmpty()) {
                ranks.add(EvidenceRank.of(category, top));
            }
        }
        return List.copyOf(ranks);
    }

    public EvidenceScore score(KnowledgeFact fact, EvidenceCategory category) {
        return score(fact, category, referenceTime);
    }

    /**
     * Puntúa un hecho contra una categoría usando el tiempo de referencia
     * indicado (sobrecarga determinista para pruebas de frescura). El valor es
     * la combinación ponderada de cobertura semántica, confianza de fuente y
     * frescura.
     */
    public EvidenceScore score(KnowledgeFact fact, EvidenceCategory category,
                               OffsetDateTime reference) {
        if (fact == null) {
            return EvidenceScore.of(0.0, category, "Hecho nulo.");
        }
        if (category == null) {
            return EvidenceScore.of(0.0, null, "Categoría nula.");
        }
        if (reference == null) {
            return EvidenceScore.of(0.0, category, "Tiempo de referencia nulo.");
        }
        String text = (fact.claim() + " " + fact.category()).toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String keyword : KEYWORDS.get(category)) {
            if (text.contains(keyword)) {
                matched++;
            }
        }
        if (matched == 0) {
            return EvidenceScore.of(0.0, category,
                "Sin coincidencia de términos con " + category.displayName() + ".");
        }
        double coverage = Math.min(matched, FULL_KEYWORD_COVERAGE) / FULL_KEYWORD_COVERAGE;
        double trust = TRUST_FACTORS.getOrDefault(fact.trust(), 0.0);
        double freshness = freshnessFactor(fact.publishedAt(), reference);
        double value = clamp(keywordWeight * coverage + trustWeight * trust
            + freshnessWeight * freshness);
        String reason = "Coincide con " + matched + " término(s) de " + category.displayName()
            + "; confianza de fuente " + fact.trust().displayName().toLowerCase(Locale.ROOT)
            + "; frescura " + String.format(Locale.ROOT, "%.1f", freshness) + ".";
        return EvidenceScore.of(value, category, reason);
    }

    /**
     * Factor determinista de frescura: datos recientes puntúan más alto; datos
     * sin fecha (publicado nulo) reciben un valor neutro {@code 0.5}.
     */
    static double freshnessFactor(OffsetDateTime publishedAt, OffsetDateTime reference) {
        if (publishedAt == null || reference == null) {
            return 0.5;
        }
        double ageDays = Math.max(0.0, ChronoUnit.DAYS.between(publishedAt, reference));
        if (ageDays <= FRESH_DAYS) {
            return 1.0;
        }
        if (ageDays <= RECENT_DAYS) {
            return 0.8;
        }
        if (ageDays <= ACCEPTABLE_DAYS) {
            return 0.6;
        }
        if (ageDays <= OLD_DAYS) {
            return 0.4;
        }
        return 0.2;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
