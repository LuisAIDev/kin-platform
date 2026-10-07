package com.kinplatform.platform.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estimador heurístico de costo de LLM (Fase 1).
 *
 * <p>Heurística documentada: tokens de entrada ≈ {@code caracteres / 4} de
 * todo el contenido del prompt (usuario + historial); tokens de salida = tope
 * máximo configurable (conservador). El costo se deriva de los precios por
 * millón de tokens de entrada y salida, inyectados desde configuración
 * ({@code deepseek.cost.input-per-1m} / {@code output-per-1m}).</p>
 *
 * <p>Si los precios no están configurados (ambos en cero), el estimador
 * devuelve cero; {@code AiBudgetControlService} documenta y desactiva el gate
 * en ese caso. No se inventa el precio de DeepSeek.</p>
 */
public final class HeuristicCostEstimator implements CostEstimator {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    private static final int CHARS_PER_TOKEN = 4;

    private final BigDecimal inputPricePer1M;
    private final BigDecimal outputPricePer1M;
    private final long maxOutputTokens;

    public HeuristicCostEstimator(BigDecimal inputPricePer1M, BigDecimal outputPricePer1M, long maxOutputTokens) {
        this.inputPricePer1M = inputPricePer1M == null ? BigDecimal.ZERO : inputPricePer1M;
        this.outputPricePer1M = outputPricePer1M == null ? BigDecimal.ZERO : outputPricePer1M;
        this.maxOutputTokens = maxOutputTokens <= 0 ? 4_096 : maxOutputTokens;
    }

    /** Tokens de entrada estimados a partir del tamaño del prompt. */
    public long estimateInputTokens(String userMessage, Iterable<? extends CharSequence> historyChunks) {
        long chars = 0;
        if (userMessage != null) {
            chars += userMessage.length();
        }
        if (historyChunks != null) {
            for (CharSequence chunk : historyChunks) {
                if (chunk != null) {
                    chars += chunk.length();
                }
            }
        }
        return Math.max(1, chars / CHARS_PER_TOKEN);
    }

    @Override
    public BigDecimal estimate(String userMessage, Iterable<? extends CharSequence> historyChunks) {
        long inputTokens = estimateInputTokens(userMessage, historyChunks);
        return costOf(inputTokens, maxOutputTokens);
    }

    @Override
    public BigDecimal costOf(long inputTokens, long outputTokens) {
        BigDecimal inputCost = inputPricePer1M
                .multiply(BigDecimal.valueOf(Math.max(0, inputTokens)))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputPricePer1M
                .multiply(BigDecimal.valueOf(Math.max(0, outputTokens)))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    @Override
    public long maxOutputTokens() {
        return maxOutputTokens;
    }

    /** Precio de entrada configurado (por millón de tokens). */
    public BigDecimal inputPricePer1M() {
        return inputPricePer1M;
    }

    /** Precio de salida configurado (por millón de tokens). */
    public BigDecimal outputPricePer1M() {
        return outputPricePer1M;
    }
}

