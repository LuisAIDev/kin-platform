package com.kinplatform.kin.reporting.report.model;

import com.kinplatform.kin.enrichment.EvidenceCategory;

/**
 * Fuente externa citada en el reporte (ADR-016, Etapa E5). Value Object
 * inmutable que referencia un hecho de conocimiento verificado: su claim,
 * la fuente (id o url) y la categoría de análisis a la que sustenta.
 */
public record CitedSource(
    String sourceId,
    String url,
    String claim,
    EvidenceCategory category,
    double score
) {

    public CitedSource {
        sourceId = sourceId == null ? "" : sourceId;
        url = url == null ? "" : url;
        claim = claim == null ? "" : claim;
        category = category == null ? EvidenceCategory.MARKET : category;
        score = Math.max(0.0, Math.min(1.0, score));
    }

    public boolean isEmpty() {
        return sourceId.isBlank() && url.isBlank() && claim.isBlank();
    }
}
