package com.kinplatform.kin.enrichment;

/**
 * Score determinista de relevancia de una evidencia para una categoría de
 * análisis (ADR-016). Inmutable.
 *
 * <p>Valor calculado en Java por {@link FactRanker}: combina la cobertura
 * semántica del hecho contra el contexto del proyecto y la confianza de la
 * fuente ({@code SourceTrust}). El valor se acota al rango {@code [0, 1]}.</p>
 */
public record EvidenceScore(
    double value,
    EvidenceCategory category,
    String reason
) {

    public EvidenceScore {
        value = Math.max(0.0, Math.min(1.0, value));
        reason = reason == null ? "" : reason;
    }

    public static EvidenceScore of(double value, EvidenceCategory category, String reason) {
        return new EvidenceScore(value, category, reason);
    }

    public boolean isRelevant(double minScore) {
        return value >= minScore;
    }
}
