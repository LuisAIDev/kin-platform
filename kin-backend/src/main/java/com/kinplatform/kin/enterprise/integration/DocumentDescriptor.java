package com.kinplatform.kin.enterprise.integration;

import java.util.List;

/**
 * Vista documental para Enterprise: metadatos de trazabilidad, resumen
 * controlado y hechos relevantes. NUNCA transporta el texto completo del
 * documento.
 */
public record DocumentDescriptor(
        String id,
        String filename,
        String mimeType,
        long size,
        String status,
        String createdAt,
        String summary,
        List<String> relevantFacts) {

    public DocumentDescriptor {
        relevantFacts = relevantFacts == null ? List.of() : List.copyOf(relevantFacts);
    }
}
