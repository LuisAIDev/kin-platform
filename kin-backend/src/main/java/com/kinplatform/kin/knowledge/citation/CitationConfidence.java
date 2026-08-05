package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.SourceTrust;

/**
 * Confianza determinista de citación derivada del nivel de confianza del hecho
 * (Fase 5 — Citation Engine). Misma escala que las métricas de confianza del
 * Knowledge Engine: pública oficial 1.0, secundaria 0.7, no verificada 0.4.
 */
public final class CitationConfidence {

    private CitationConfidence() {
    }

    public static double of(SourceTrust trust) {
        if (trust == null) {
            return 0.0;
        }
        return switch (trust) {
            case OFFICIAL_PUBLIC -> 1.0;
            case SECONDARY -> 0.7;
            case UNVERIFIED -> 0.4;
        };
    }
}
