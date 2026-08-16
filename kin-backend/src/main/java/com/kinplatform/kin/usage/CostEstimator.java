package com.kinplatform.kin.usage;

import java.math.BigDecimal;

/**
 * Estimador de costo de una llamada LLM. La implementación inicial es una
 * heurística documentada (tokens ≈ caracteres / 4 con tope de salida
 * configurable); puede reemplazarse por un estimador más preciso sin cambiar
 * los consumidores. El costo se calcula con precios configurados por millón
 * de tokens — nunca hardcodeados.
 */
public interface CostEstimator {

    /** Estimación de costo (USD) de un turno de chat. */
    BigDecimal estimate(String userMessage, Iterable<? extends CharSequence> historyChunks);

    /** Costo (USD) de tokens reales con los precios configurados. */
    BigDecimal costOf(long inputTokens, long outputTokens);

    /** Tope de tokens de salida usado por la heurística (peor caso por llamada). */
    long maxOutputTokens();
}
