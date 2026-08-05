package com.kinplatform.kin.knowledge.citation;

/**
 * Decisión inmutable de la política de citación para un hecho (Fase 5 —
 * Citation Engine): incluir o excluir la cita, con motivo auditable y confianza.
 */
public record CitationDecision(
    boolean included,
    String sourceId,
    String url,
    String reason,
    double confidence
) {

    public CitationDecision {
        sourceId = sourceId == null ? "" : sourceId;
        url = url == null ? "" : url;
        reason = reason == null ? "" : reason;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public static CitationDecision excluded(String reason) {
        return new CitationDecision(false, "", "", reason, 0.0);
    }
}
