package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Plan de mercado del proyecto empresarial (value object).
 *
 * <p>Representa el análisis de mercado: TAM, SAM y SOM (tamaño de mercado,
 * mercado direccionable y mercado obtenible), tasa de crecimiento, competidores
 * verificados, canales de distribución, barreras de entrada, segmentos de
 * cliente y la confianza de la estimación. Producido por
 * {@code MarketEngine}.</p>
 */
public record MarketPlan(
    double tam,
    double sam,
    double som,
    double growthRate,
    List<String> competitors,
    List<String> channels,
    List<String> entryBarriers,
    List<String> customerSegments,
    double confidence
) {

    public MarketPlan {
        ValueObjects.requireNonNegative(tam, "tam");
        ValueObjects.requireNonNegative(sam, "sam");
        ValueObjects.requireNonNegative(som, "som");
        ValueObjects.requireNonNegative(growthRate, "growthRate");
        competitors = ValueObjects.immutableNotBlank(competitors, "competitors");
        channels = ValueObjects.immutableNotBlank(channels, "channels");
        entryBarriers = ValueObjects.immutableNotBlank(entryBarriers, "entryBarriers");
        customerSegments = ValueObjects.immutableNotBlank(customerSegments, "customerSegments");
        ValueObjects.requireInRange(confidence, 0.0, 1.0, "confidence");
    }

    /**
     * Crea un plan de mercado vacío (mercados en cero y listas vacías).
     */
    public static MarketPlan empty() {
        return new MarketPlan(0.0, 0.0, 0.0, 0.0,
            List.of(), List.of(), List.of(), List.of(), 0.0);
    }

    /**
     * Crea un plan de mercado completo.
     */
    public static MarketPlan of(double tam, double sam, double som, double growthRate,
                                List<String> competitors, List<String> channels,
                                List<String> entryBarriers, List<String> customerSegments,
                                double confidence) {
        return new MarketPlan(tam, sam, som, growthRate, competitors, channels,
            entryBarriers, customerSegments, confidence);
    }
}
